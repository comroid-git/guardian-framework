package org.comroid.mutatio.span;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.cache.CachedValue;
import org.comroid.mutatio.pipe.BasicPipe;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static java.util.Objects.nonNull;

public class Span<T> extends CachedValue.Abstract<T> implements Collection<T>, ReferenceIndex<T>, Reference<T> {
    public static final int UNFIXED_SIZE = -1;
    public static final DefaultModifyPolicy DEFAULT_MODIFY_POLICY = DefaultModifyPolicy.SKIP_NULLS;
    private static final Span<?> EMPTY = new Span<>(ReferenceIndex.empty(), DefaultModifyPolicy.IMMUTABLE);
    private final Object dataLock = Polyfill.selfawareLock();
    private final ReferenceIndex<T> storage;
    private final int fixedCapacity;
    private final ModifyPolicy modifyPolicy;


    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    public final boolean isSingle() {
        return size() == 1;
    }

    public final boolean isNotSingle() {
        return size() != 1;
    }

    public final boolean isFixedSize() {
        return fixedCapacity != UNFIXED_SIZE;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    public Span() {
        this(ReferenceIndex.create(), UNFIXED_SIZE, DEFAULT_MODIFY_POLICY);
    }

    public Span(int fixedCapacity) {
        this(ReferenceIndex.create(), fixedCapacity, DEFAULT_MODIFY_POLICY);
    }

    public Span(ReferenceIndex<T> referenceIndex, ModifyPolicy modifyPolicy) {
        this(referenceIndex, UNFIXED_SIZE, modifyPolicy);
    }

    public Span(ReferenceIndex<? extends T> data, ModifyPolicy modifyPolicy, boolean fixedSize) {
        this(data, fixedSize ? data.size() : UNFIXED_SIZE, modifyPolicy);
    }

    protected Span(ReferenceIndex<? extends T> data, int fixedCapacity, ModifyPolicy modifyPolicy) {
        super(null);

        //noinspection unchecked
        this.storage = (ReferenceIndex<T>) data;
        this.fixedCapacity = fixedCapacity;
        this.modifyPolicy = modifyPolicy;
    }

    public static <T> Collector<T, ?, Span<T>> collector() {
        return Span.<T>make()
                .fixedSize(true)
                .modifyPolicy(DefaultModifyPolicy.IMMUTABLE)
                .collector();
    }

    public static <T> Span.API<T> make() {
        return new Span.API<>();
    }

    public static <T> Span<T> empty() {
        //noinspection unchecked
        return (Span<T>) EMPTY;
    }

    public static <T> Span<T> singleton(T it) {
        return Span.<T>make().initialValues(it)
                .fixedSize(true)
                .modifyPolicy(DefaultModifyPolicy.IMMUTABLE)
                .span();
    }

    public static <T> Span<T> immutable(Collection<T> of) {
        return Span.<T>make()
                .modifyPolicy(DefaultModifyPolicy.IMMUTABLE)
                .initialValues(of)
                .fixedSize(true)
                .span();
    }

    @SafeVarargs
    public static <T> Span<T> immutable(T... of) {
        return Span.<T>make().initialValues(of)
                .fixedSize(true)
                .modifyPolicy(DefaultModifyPolicy.IMMUTABLE)
                .span();
    }

    @Override
    public boolean contains(Object o) {
        for (T it : this) {
            if (o.equals(it)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> objects) {
        return objects.stream()
                .allMatch(this::contains);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> objects) {
        boolean added = false;

        for (T object : objects) {
            if (add(object) && !added) {
                added = true;
            }
        }

        return added;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> objects) {
        boolean removed = false;

        for (Object object : objects) {
            if (remove(object) && !removed) {
                removed = true;
            }
        }

        return removed;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> keep) {
        boolean removed = false;

        if (keep.size() > size()) {
            for (Object k : keep) {
                for (T each : this) {
                    if (!k.equals(each) && remove(each)) {
                        removed = true;
                    }
                }
            }
        } else {
            for (T each : this) {
                for (Object k : keep) {
                    if (!k.equals(each) && remove(each)) {
                        removed = true;
                    }
                }
            }
        }

        return removed;
    }

    @Override
    public List<T> unwrap() {
        //noinspection unchecked
        return Arrays.asList((T[]) toArray());
    }

    @Override
    @Contract
    public final int size() {
        return storage.size();
    }

    @NotNull
    @Override
    public final Iterator iterator() {
        synchronized (dataLock) {
            return new Iterator();
        }
    }

    @NotNull
    @Override
    public final Object[] toArray() {
        return toArray(new Object[0], Function.identity());
    }

    @Override
    public final <R> @NotNull R[] toArray(@NotNull R[] dummy) {
        //noinspection unchecked
        return toArray(dummy, it -> (R) it);
    }

    @Override
    public final synchronized boolean add(T it) {
        synchronized (dataLock) {
            int i;

            for (i = 0; i < storage.size(); i++) {
                final T oldV = valueAt(i);
                if (modifyPolicy.canOverwrite(oldV, it)) {
                    storage.remove(oldV);
                    storage.add(it);
                    return true;
                }
            }

            if (isFixedSize()) {
                throw new IndexOutOfBoundsException("Span cannot be resized");
            }
            if (i != storage.size()) {
                throw new AssertionError(String.format(
                        "Suspicious Span.add() call: index too large {expected: %d, actual: %d}%n",
                        storage.size(), i
                ));
            }

            return modifyPolicy.canInitialize(it) && storage.add(it);
        }
    }

    public Processor<T> process(int index) {
        return getReference(index).process();
    }

    @Override
    public final synchronized boolean remove(Object other) {
        synchronized (dataLock) {
            if (isFixedSize())
                throw new UnsupportedOperationException("Span has fixed size; cannot remove");

            for (int i = 0; i < storage.size(); i++) {
                final T valueAt = valueAt(i);

                if (valueAt == null)
                    throw new AssertionError();

                if (valueAt.equals(other)) {
                    if (!modifyPolicy.canRemove(other))
                        modifyPolicy.fail("removing " + other);

                    return storage.remove(valueAt);
                }
            }

            return false;
        }
    }

    @Override
    public final void clear() {
        synchronized (dataLock) {
            storage.clear();
        }
    }

    @Override
    public Stream<T> stream() {
        return Stream.of(toArray()).map(Polyfill::uncheckedCast);
    }

    @Override
    public Pipe<T> pipe() {
        return new BasicPipe<>(this, 512);
    }

    @Override
    public void rebind(Supplier<T> behind) {
        throw new UnsupportedOperationException("Cannot rebind Span");
    }

    @Override
    public Reference<T> getReference(int index) {
        return storage.getReference(index);
    }

    private <R> R[] toArray(R[] dummy, Function<Object, R> castOP) {
        synchronized (dataLock) {
            final R[] yields = Arrays.copyOf(dummy, size());

            for (int i, c = i = 0; i < storage.size(); i++) {
                final T valueAt = valueAt(i);

                if (modifyPolicy.canIterate(valueAt)) {
                    yields[c++] = castOP.apply(valueAt);
                }
            }

            return yields;
        }
    }

    private @Nullable T valueAt(int index) {
        synchronized (dataLock) {
            if (index >= size()) {
                return null;
            }

            T cast = storage.get(index);
            return modifyPolicy.canIterate(cast) ? cast : null;
        }
    }

    @Override
    public final String toString() {
        @NotNull Object[] arr = toArray();
        return String.format("Span{nullPolicy=%s, data={%d}%s}", modifyPolicy, arr.length, Arrays.toString(arr));
    }

    @Nullable
    @Override
    public final T get() {
        synchronized (dataLock) {
            for (int i = 0; i < storage.size(); i++) {
                final T valueAt = valueAt(i);

                if (modifyPolicy.canIterate(valueAt)) {
                    return valueAt;
                }
            }

            return null;
        }
    }

    public final @NotNull T requireSingle() {
        if (!isSingle())
            throw new AssertionError("Span does not hold exactly one element!");
        return requireNonNull();
    }

    @Override
    public @NotNull T requireNonNull() throws NullPointerException {
        return requireNonNull("No iterable value present");
    }

    public Span<T> range(int startIncl, int endExcl) {
        synchronized (dataLock) {
            return new Span<>(storage.subset(startIncl, endExcl), DefaultModifyPolicy.IMMUTABLE);
        }
    }

    @Contract(mutates = "this")
    public void sort(Comparator<T> comparator) {
        synchronized (dataLock) {
            final List<T> list = new ArrayList<>(storage.unwrap());
            list.sort(comparator);

            storage.clear();
            list.forEach(storage::add);
        }

        cleanup();
    }

    @Contract(mutates = "this")
    public final synchronized void cleanup() {
        synchronized (dataLock) {
            if (isFixedSize()) {
                final ArrayList<T> list = new ArrayList<>(storage.unwrap());

                storage.clear();
                list.stream()
                        .filter(it -> !modifyPolicy.canCleanup(it))
                        .forEachOrdered(storage::add);
            } else {
                final @NotNull Object[] array = toArray();

                storage.clear();
                for (Object it : array) //noinspection unchecked
                    storage.add((T) it);
            }
        }
    }

    @Contract("-> new")
    public final API<T> reconfigure(/* todo: boolean parameter finalizeOldSpan? */) {
        return new API<>(storage);
    }

    public final <C extends Collection<T>> C into(Supplier<C> collectionSupplier) {
        final C coll = collectionSupplier.get();
        coll.addAll(this);

        return coll;
    }

    @SuppressWarnings("Convert2MethodRef")
    public enum DefaultModifyPolicy implements ModifyPolicy {
        //endformatting
        SKIP_NULLS(init -> true,
                iterate -> nonNull(iterate),
                (overwriting, with) -> Objects.isNull(overwriting),
                remove -> true,
                cleanup -> Objects.isNull(cleanup)
        ),

        NULL_ON_INIT(init -> true,
                iterate -> nonNull(iterate),
                (overwriting, with) -> nonNull(with) && Objects.isNull(overwriting),
                remove -> true,
                cleanup -> Objects.isNull(cleanup)
        ),

        PROHIBIT_NULLS(init -> {
            Objects.requireNonNull(init);
            return true;
        },
                iterate -> nonNull(iterate),
                (overwriting, with) -> nonNull(with) && Objects.isNull(overwriting),
                remove -> true,
                cleanup -> Objects.isNull(cleanup)
        ),

        IMMUTABLE(init -> true, iterate -> true, (overwriting, with) -> false, remove -> false, cleanup -> false);
        //startformatting

        private final static Object dummy = new Object();
        private final Predicate<Object> initVarTester;
        private final Predicate<Object> iterateVarTester;
        private final BiPredicate<Object, Object> overwriteTester;
        private final Predicate<Object> removeTester;
        private final Predicate<Object> cleanupTester;

        DefaultModifyPolicy(
                Predicate<Object> initVarTester,
                Predicate<Object> iterateVarTester,
                BiPredicate<Object, Object> overwriteTester,
                Predicate<Object> removeTester,
                Predicate<Object> cleanupTester
        ) {

            this.initVarTester = initVarTester;
            this.iterateVarTester = iterateVarTester;
            this.overwriteTester = overwriteTester;
            this.removeTester = removeTester;
            this.cleanupTester = cleanupTester;
        }

        @Override
        public boolean canInitialize(Object var) {
            return var != dummy && initVarTester.test(var);
        }

        @Override
        public boolean canIterate(Object var) {
            return var != dummy && iterateVarTester.test(var);
        }

        @Override
        public boolean canOverwrite(Object old, Object with) {
            return (old != dummy && with != dummy) && overwriteTester.test(old, with);
        }

        @Override
        public boolean canRemove(Object var) {
            return var != dummy && removeTester.test(var);
        }

        @Override
        public boolean canCleanup(Object var) {
            return var != dummy && cleanupTester.test(var);
        }

        @Override
        public void fail(String message) throws NullPointerException {
            throw new NullPointerException(String.format("NullPolicy %s was violated: %s", name(), message));
        }
    }

    //region API Class
    public static final class API<T> {
        private static final int RESULT_FIXED_SIZE = -2;
        private final ReferenceIndex<T> storage;
        private ModifyPolicy modifyPolicy = Span.DEFAULT_MODIFY_POLICY;
        private int fixedSize;

        public API() {
            this(ReferenceIndex.create());
        }

        private API(ReferenceIndex<T> storage) {
            this.storage = storage;
        }

        public Collector<T, ?, Span<T>> collector() {
            class SpanCollector implements Collector<T, Span<T>, Span<T>> {
                private final Supplier<Span<T>> supplier = Span::new;
                private final BiConsumer<Span<T>, T> accumulator = Span::add;
                private final BinaryOperator<Span<T>> combiner = (ts, ts2) -> {
                    ts.addAll(ts2);
                    return ts;
                };
                private final Function<Span<T>, Span<T>> finisher = new Function<Span<T>, Span<T>>() {
                    @Override
                    public Span<T> apply(Span<T> ts) {
                        ts.forEach(storage::add);
                        return span();
                    }
                };

                @Override
                public Supplier<Span<T>> supplier() {
                    return supplier;
                }

                @Override
                public BiConsumer<Span<T>, T> accumulator() {
                    return accumulator;
                }

                @Override
                public BinaryOperator<Span<T>> combiner() {
                    return combiner;
                }

                @Override
                public Function<Span<T>, Span<T>> finisher() {
                    return finisher;
                }

                @Override
                public Set<Characteristics> characteristics() {
                    return Collections.singleton(Characteristics.IDENTITY_FINISH);
                }
            }

            return new SpanCollector();
        }

        public Span<T> span() {
            return new Span<>(storage, modifyPolicy, fixedSize == API.RESULT_FIXED_SIZE);
        }

        @Contract(value = "_ -> this", mutates = "this")
        public API<T> initialValues(Collection<T> values) {
            values.forEach(storage::add);

            return this;
        }

        @Deprecated
        public API<T> initialSize(int initialSize) {
            return fixedSize(initialSize);
        }

        @Contract(value = "_ -> this", mutates = "this")
        public API<T> fixedSize(int fixedSize) {
            this.fixedSize = fixedSize;

            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public API<T> fixedSize(boolean fixedSize) {
            this.fixedSize = API.RESULT_FIXED_SIZE;

            return this;
        }

        @Contract(value = "_ -> this", mutates = "this")
        public API<T> modifyPolicy(ModifyPolicy modifyPolicy) {
            this.modifyPolicy = modifyPolicy;

            return this;
        }

        @SafeVarargs
        @Contract(value = "_ -> this", mutates = "this")
        public final API<T> initialValues(T... values) {
            return initialValues(Arrays.asList(values));
        }
    }

    public final class Iterator implements java.util.Iterator<T> {
        private final Object[] dataSnapshot = toArray();
        private int previousIndex = -1;
        private @Nullable Object next;

        @Override
        public boolean hasNext() {
            return next != null || tryAcquireNext();
        }

        @Override
        public T next() {
            if (next != null || tryAcquireNext()) {
                //noinspection unchecked
                final T it = (T) next;
                next = null;

                return it;
            }

            throw new NoSuchElementException("Span had no more iterable values!");
        }

        private boolean tryAcquireNext() {
            int nextIndex = previousIndex + 1;
            if (next != null || nextIndex >= dataSnapshot.length) {
                return false;
            }

            next = dataSnapshot[nextIndex];

            while (!modifyPolicy.canIterate(next)) {
                if (nextIndex + 1 >= dataSnapshot.length)
                    return false;
            }

            previousIndex = nextIndex;
            return next != null;
        }
    }
}
