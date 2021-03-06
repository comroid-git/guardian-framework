package org.comroid.mutatio.span;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.Structure;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Deprecated
public final class Span<T> extends ReferenceList<T> implements Rewrapper<T> {
    public static final int UNFIXED_SIZE = -1;
    public static final DefaultModifyPolicy DEFAULT_MODIFY_POLICY = DefaultModifyPolicy.SKIP_NULLS;
    private static final Span<?> EMPTY = new Span<>(ReferenceList.empty(), DefaultModifyPolicy.IMMUTABLE);
    private final Object dataLock = Polyfill.selfawareObject();
    private final ReferenceList<T> storage;
    private final int fixedCapacity;
    private final ModifyPolicy modifyPolicy;

    public final boolean isSingle() {
        return size() == 1;
    }

    public final boolean isNotSingle() {
        return size() != 1;
    }

    public final boolean isFixedSize() {
        return fixedCapacity != UNFIXED_SIZE;
    }

    public Span() {
        this(ReferenceList.create(), UNFIXED_SIZE, DEFAULT_MODIFY_POLICY);
    }

    public Span(int fixedCapacity) {
        this(ReferenceList.create(), fixedCapacity, DEFAULT_MODIFY_POLICY);
    }

    public Span(RefContainer<?, T> referenceList, ModifyPolicy modifyPolicy) {
        this(referenceList, UNFIXED_SIZE, modifyPolicy);
    }

    public Span(ReferenceList<T> data, ModifyPolicy modifyPolicy, boolean fixedSize) {
        this(data, fixedSize ? data.size() : UNFIXED_SIZE, modifyPolicy);
    }

    protected Span(RefContainer<?, T> data, int fixedCapacity, ModifyPolicy modifyPolicy) {
        super(Objects.requireNonNull(data, "storage adapter is null"));

        this.storage = new ReferenceList<>(data);
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

    public List<T> unwrap() {
        //noinspection unchecked
        return Arrays.asList((T[]) toArray());
    }

    @Override
    public final synchronized boolean add(T it) {
        synchronized (dataLock) {
            cleanup();
            return super.add(it);
        }
    }

    @Override
    public boolean addReference(KeyedReference<@NotNull Integer, T> ref) {
        return storage.addReference(ref);
    }

    @Deprecated
    public Reference<T> process(int index) {
        return getReference(index);
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
        return requireNonNull(() -> "No iterable value present");
    }

    @Override
    public void sort(Comparator<? super T> comparator) {
        super.comparator = Structure.wrapComparator(comparator);
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

    public void cleanup() {
        streamRefs().filter(Rewrapper::isNull)
                .map(KeyedReference::getKey)
                .collect(Collectors.toList())
                .forEach(this::removeRef);
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
        private final ReferenceList<T> storage;
        private ModifyPolicy modifyPolicy = Span.DEFAULT_MODIFY_POLICY;
        private int fixedSize;

        public API() {
            this(ReferenceList.create(true));
        }

        private API(ReferenceList<T> storage) {
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
                        ts.forEach((Consumer<T>) storage::add);
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
