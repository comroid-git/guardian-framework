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

    public static <T> ReferenceIndex<?, T> create() {
        return of(new ArrayList<>());
    }

    public static <T> ReferenceIndex<?, T> of(Collection<T> list) {
        ReferenceIndex<?, T> refs = new ReferenceIndex<>();
        refs.addAll(list);
        return refs;
    }

    public static <T> ReferenceIndex<?, T> empty() {
        //noinspection unchecked
        return (ReferenceIndex<Object, T>) Support.EMPTY;
    }

    @SafeVarargs
    public static <T> ReferenceIndex<?, T> of(T... values) {
        return of(Arrays.asList(values));
    }

    /*
        public <X> ReferenceIndex<X, T> bi(Function<T, X> source) {
            return new KeyedPipe<>(this, BiStageAdapter.source(source), autoEmptyLimit);
        }
    */

    public final <R> ReferenceIndex<T, R> addStage(StageAdapter<T, R> stage) {
        ReferenceIndex<T, R> sub = new ReferenceIndex<>(this, stage);
        addDependent(sub);
        return sub;
    }

    public @Nullable Reference<T> getReference(int index) {
        return getReference(index, false);
    }

    @Override
    public void close() {
        for (ValueCache<?> valueCache : getDependents())
            if (valueCache instanceof UncheckedCloseable)
                ((UncheckedCloseable) valueCache).close();
    }

    @NotNull
    @Override
    public final ReferenceIndex<?, T> subList(int fromIndex, int toIndex) {
        return new Support.OfRange<>(this, fromIndex, toIndex);
    }

    @OverrideOnly
    public boolean addReference(int index, Reference<T> ref) {
        return putAccessor(index, ref);
    }

    public final ReferenceIndex<T, T> filter(Predicate<? super T> predicate) {
        return addStage(StageAdapter.filter(predicate));
    }

    public final ReferenceIndex<T, T> yield(Predicate<? super T> predicate, Consumer<? super T> elseConsume) {
        return filter(it -> {
            if (predicate.test(it))
                return true;
            elseConsume.accept(it);
            return false;
        });
    }

    public final <R> ReferenceIndex<T, R> map(Function<? super T, ? extends R> mapper) {
        return addStage(StageAdapter.map(mapper));
    }

    public final <R> ReferenceIndex<T, R> flatMap(Class<R> target) {
        return filter(target::isInstance).map(target::cast);
    }

    public final <R> ReferenceIndex<T, R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return addStage(StageAdapter.flatMap(mapper));
    }

    public final ReferenceIndex<T, T> peek(Consumer<? super T> action) {
        return addStage(StageAdapter.peek(action));
    }

    public final void forEach(Consumer<? super T> action) {
        //noinspection SimplifyStreamApiCallChains
        stream().forEach(action);
    }

    @Deprecated // todo: fix
    public final ReferenceIndex<T, T> distinct() {
        return addStage(StageAdapter.distinct());
    }

    @Deprecated // todo: fix
    public final ReferenceIndex<T, T> limit(long maxSize) {
        return addStage(StageAdapter.limit(maxSize));
    }

    @Deprecated // todo: fix
    public final ReferenceIndex<T, T> skip(long skip) {
        return addStage(StageAdapter.skip(skip));
    }

    public final ReferenceIndex<T, T> sorted() {
        return sorted(Polyfill.uncheckedCast(Comparator.naturalOrder()));
    }

    public final ReferenceIndex<T, T> sorted(Comparator<? super T> comparator) {
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

    public final boolean remove(Object item) {
        for (int i = 0; i < this.size(); i++) {
            Reference<T> ref = getReference(i);
            if (ref == null)
                continue;
            if (ref.contentEquals(item))
                return removeRef(i);
        }
        return false;
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
    public boolean add(T t) {
        Reference<T> ref = getReference(size(), true);
        return ref.set(t);
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
        if (removeRef(index))
        return old;
        else return null;
    }

    @Override
    public final int indexOf(Object other) {
        for (int i = 0; i < size(); i++) {
            T each = get(i);
            if (other.equals(each))
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

    @Deprecated
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
        final ReferenceIndex<T, T> resulting = addStage(stage);
        stage.future.thenRun(ThrowingRunnable.handling(resulting::close, null));

        return stage.future;
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
