package org.comroid.mutatio.ref;

import org.comroid.abstr.AbstractList;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.api.ThrowingRunnable;
import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.pipe.StageAdapter;
import org.comroid.mutatio.pipe.impl.SortedResultingPipe;
import org.comroid.mutatio.span.Span;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class ReferenceIndex<In, T>
        extends ReferenceAtlas.ForList<In, T>
        implements AbstractList<T>, Pipeable<T>, UncheckedCloseable {
    public ReferenceIndex() {
        this(null, StageAdapter.identity());
    }

    public ReferenceIndex(
            @NotNull ReferenceIndex<?, T> parent
    ) {
        this(Polyfill.uncheckedCast(parent), StageAdapter.identity());
    }

    public ReferenceIndex(
            @Nullable ReferenceIndex<?, In> parent,
            @NotNull StageAdapter<In, T> advancer
    ) {
        super(parent, advancer);
    }

    public static <T> ReferenceIndex<Object, T> create() {
        return of(new ArrayList<>());
    }

    public static <T> ReferenceIndex<Object, T> of(List<T> list) {
        Support.OfList<T> index = new Support.OfList<>();
        list.forEach(index::add);
        return index;
    }

    public static <T> ReferenceIndex<Object, T> empty() {
        //noinspection unchecked
        return (ReferenceIndex<Object, T>) Support.EMPTY;
    }

    @SafeVarargs
    public static <T> ReferenceIndex<Object, T> of(T... values) {
        return of(Arrays.asList(values));
    }

    public static <T> ReferenceIndex<Object, T> of(Collection<T> collection) {
        final ReferenceIndex<Object, T> pipe = create();
        collection.forEach(pipe::add);

        return pipe;
    }

    public static <T> ReferenceIndex<Object, T> ofStream(Stream<T> stream) {
        final ReferenceIndex<Object, T> pipe = create();
        stream.iterator().forEachRemaining(pipe::add);
        return pipe;
    }

    /*
        public <X> ReferenceIndex<X, T> bi(Function<T, X> source) {
            return new KeyedPipe<>(this, BiStageAdapter.source(source), autoEmptyLimit);
        }
    */

    public final <R> ReferenceIndex<Object, R> addStage(Reference.Advancer<T, R> stage) {
        return addStage(stage, null);
    }

    public final <R> ReferenceIndex<Object, R> addStage(Reference.Advancer<T, R> stage, Function<R, T> reverser) {
        ReferenceIndex<Object, R> sub = new Support.WithStage<>(this, stage, reverser);
        addDependent(sub);
        return sub;
    }

    public Reference<T> getReference(int index) {
        Reference<T> ref = computeRef(index);
        if (ref == null) ref = Reference.empty();
        return ref;
    }

    @Override
    public void close() {
        for (ValueCache<?> valueCache : getDependents())
            if (valueCache instanceof UncheckedCloseable)
                ((UncheckedCloseable) valueCache).close();
    }

    @NotNull
    @Override
    public final ReferenceIndex<Object, T> subList(int fromIndex, int toIndex) {
        return new Support.OfRange<>(this, fromIndex, toIndex);
    }

    @Deprecated
    public final List<T> unwrap() {
        //noinspection SimplifyStreamApiCallChains
        return stream().collect(Collectors.toList());
    }

    @OverrideOnly
    public boolean addReference(int index, Reference<T> ref) {
        computeRef(index).rebind(ref);
        return true;
    }

    public final ReferenceIndex<Object, T> filter(Predicate<? super T> predicate) {
        return addStage(StageAdapter.filter(predicate));
    }

    public final ReferenceIndex<Object, T> yield(Predicate<? super T> predicate, Consumer<? super T> elseConsume) {
        return filter(it -> {
            if (predicate.test(it))
                return true;
            elseConsume.accept(it);
            return false;
        });
    }

    public final <R> ReferenceIndex<Object, R> map(Function<? super T, ? extends R> mapper) {
        return addStage(StageAdapter.map(mapper));
    }

    public final <R> ReferenceIndex<Object, R> flatMap(Class<R> target) {
        return filter(target::isInstance).map(target::cast);
    }

    public final <R> ReferenceIndex<Object, R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return addStage(StageAdapter.flatMap(mapper));
    }

    public final ReferenceIndex<Object, T> peek(Consumer<? super T> action) {
        return addStage(StageAdapter.peek(action));
    }

    public final void forEach(Consumer<? super T> action) {
        //noinspection SimplifyStreamApiCallChains
        stream().forEach(action);
    }

    @Deprecated // todo: fix
    public final ReferenceIndex<Object, T> distinct() {
        return addStage(StageAdapter.distinct());
    }

    @Deprecated // todo: fix
    public final ReferenceIndex<Object, T> limit(long maxSize) {
        return addStage(StageAdapter.limit(maxSize));
    }

    @Deprecated // todo: fix
    public final ReferenceIndex<Object, T> skip(long skip) {
        return addStage(StageAdapter.skip(skip));
    }

    public final ReferenceIndex<Object, T> sorted() {
        return sorted(Polyfill.uncheckedCast(Comparator.naturalOrder()));
    }

    public final ReferenceIndex<Object, T> sorted(Comparator<? super T> comparator) {
        return new SortedResultingPipe<>(this, comparator);
    }

    @NotNull
    public final Reference<T> findFirst() {
        return sorted().findAny();
    }

    @NotNull
    public final Reference<T> findAny() {
        return Reference.conditional(() -> size() > 0, () -> span().get(0));
    }

    public boolean remove(Object item) throws UnsupportedOperationException {
        validateBaseExists();
        //noinspection unchecked
        Object it = reverser.apply((T) item);
        return base.remove(it);
    }

    public final Stream<T> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    @Nullable
    public final T get(int index) {
        return getReference(index).get();
    }

    public final Optional<T> wrap(int index) {
        return getReference(index).wrap();
    }

    public final @NotNull T requireNonNull(int index) throws NullPointerException {
        return getReference(index).requireNonNull();
    }

    public final @NotNull T requireNonNull(int index, String message) throws NullPointerException {
        return getReference(index).requireNonNull(message);
    }

    public final @NotNull T requireNonNull(int index, Supplier<String> message) throws NullPointerException {
        return getReference(index).requireNonNull(message);
    }

    @Override
    public final boolean contains(Object other) {
        return stream().anyMatch(other::equals);
    }

    @Override
    public final T set(int index, T element) {
        return getReference(index, true).replace(element);
    }

    @Override
    public final void add(int index, T element) {
        addReference(index, Reference.constant(element));
    }

    @Override
    public final T remove(int index) {
        T old = get(index);
        base.remove(index);
        accessors.remove(index);
        return old;
    }

    @Override
    public final int indexOf(Object other) {
        for (int i = 0; i < size(); i++) {
            T each = get(i);
            if (each.equals(other))
                return i;
        }
        return -1;
    }

    @Override
    public final int lastIndexOf(Object other) {
        for (int i = size() - 1; i > -1; i--) {
            T each = get(i);
            if (other.equals(each))
                return i;
        }
        return -1;
    }

    @NotNull
    @Override
    public final ListIterator<T> listIterator(int index) {
        return new RefIndIterator(index);
    }

    public Span<T> span() {
        return new Span<>(this, Span.DefaultModifyPolicy.SKIP_NULLS);
    }

    public CompletableFuture<T> next() {
        class OnceCompletingStage implements StageAdapter<T, T> {
            private final CompletableFuture<T> future = new CompletableFuture<>();

            @Override
            public boolean isIdentityValue() {
                return false;
            }

            @Override
            public Reference<T> advance(Reference<T> ref) {
                if (!ref.isNull() && !future.isDone())
                    future.complete(ref.get());
                return Reference.empty();
            }

            @Override
            public T advanceValue(T value) {
                return value;
            }
        }

        final OnceCompletingStage stage = new OnceCompletingStage();
        final ReferenceIndex<Object, T> resulting = addStage(stage);
        stage.future.thenRun(ThrowingRunnable.handling(resulting::close, null));

        return stage.future;
    }

    @Override
    protected Reference<T> createEmptyRef(@NotNull Integer key) {
        return KeyedReference.createKey(key);
    }

    private static final class Support {
        public static final ReferenceIndex<Object, ?> EMPTY = ReferenceIndex.of(Collections.emptyList());

        private static class WithStage<T, R> extends ReferenceIndex<Object, R> {
            private WithStage(ReferenceIndex<Object, T> base, Reference.Advancer<T, R> stage, Function<R, T> reverser) {
                super(base, stage, reverser);
            }
        }

        private static final class OfList<T> extends ReferenceIndex<Object, T> {
            private final List<Reference<T>> list;

            private OfList() {
                this(new ArrayList<>());
            }

            private OfList(List<Reference<T>> list) {
                this.list = list;
            }

            @Override
            public boolean addReference(int index, Reference<T> ref) {
                list.add(ref);
                updateCache();
                return true;
            }

            @Override
            public boolean remove(Object item) {
                if (list.removeIf(ref -> ref.contentEquals(item))) {
                    updateCache();
                    return true;
                }
                return false;
            }

            @Override
            public void clear() {
                list.clear();
                updateCache();
            }

            @Override
            public Stream<Reference<T>> streamRefs() {
                return list.stream();
            }

            @Override
            public Reference<T> getReference(final int index) {
                return list.get(index);
            }

            @Override
            public int size() {
                return list.size();
            }
        }

        public static class OfRange<T> extends ReferenceIndex<Object, T> {
            public OfRange(ReferenceIndex<Object, T> base, int fromIndex, int toIndex) {
                // todo
            }
        }
    }

    private class RefIndIterator implements ListIterator<T> {
        private final Reference<Integer> index;
        private final Reference<T> next;
        private final Reference<T> prev;

        private RefIndIterator(int initialIndex) {
            this.index = Reference.create(initialIndex);
            this.next = index.map(ind -> {
                for (int i = ind; i < size(); i++) {
                    T it = get(i);
                    if (it != null)
                        return it;
                }
                return null;
            }).filter(Objects::nonNull);
            this.prev = index.map(ind -> {
                for (int i = ind; i > -1; i--) {
                    T it = get(i);
                    if (it != null)
                        return it;
                }
                return null;
            }).filter(Objects::nonNull);
        }

        @Override
        public boolean hasNext() {
            return next.isNonNull();
        }

        @Override
        public T next() {
            index.compute(x -> x + 1);
            return next.assertion();
        }

        @Override
        public boolean hasPrevious() {
            return prev.isNonNull();
        }

        @Override
        public T previous() {
            index.compute(x -> x - 1);
            return prev.assertion();
        }

        @Override
        public int nextIndex() {
            if (index.contentEquals(size()))
                return size();
            return index.into(x -> x + 1);
        }

        @Override
        public int previousIndex() {
            if (index.contentEquals(0))
                return 0;
            return index.into(x -> x - 1);
        }

        @Override
        public void remove() {
            index.consume(ReferenceIndex.this::remove);
        }

        @Override
        public void set(T t) {
            if (!index.test(i -> ReferenceIndex.this.getReference(i).set(t)))
                throw new UnsupportedOperationException("Unable to set");
        }

        @Override
        public void add(T t) {
            index.ifPresent(i -> ReferenceIndex.this.add(i, t));
        }
    }
}
