package org.comroid.mutatio.ref;

import org.comroid.abstr.AbstractList;
import org.comroid.api.Rewrapper;
import org.comroid.api.ThrowingRunnable;
import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.adapter.StageAdapter;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.span.Span;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.uncheckedCast;

public class ReferenceList<T>
        extends ReferencePipe.ForList<Object, T>
        implements AbstractList<T>, Pipeable<T>, UncheckedCloseable {
    private static final ReferenceList<Void> EMPTY = new ReferenceList<Void>() {{
        setImmutable();
    }};
    private final int fromIndex;
    private final int toIndex;

    public ReferenceList() {
        this(null);
    }

    public ReferenceList(
            @NotNull ReferenceList<T> parent
    ) {
        this(uncheckedCast(parent), StageAdapter.identity());
    }

    public <In> ReferenceList(
            @Nullable ReferenceList<In> parent,
            @NotNull StageAdapter<In, T> advancer
    ) {
        this(parent, advancer, null);
    }

    public <In> ReferenceList(
            @Nullable ReferenceList<In> parent,
            @NotNull StageAdapter<In, T> advancer,
            @Nullable Comparator<KeyedReference<@NotNull Integer, T>> comparator
    ) {
        super(uncheckedCast(parent), uncheckedCast(advancer), comparator);

        this.fromIndex = -1;
        this.toIndex = -1;
    }

    private ReferenceList(
            @NotNull ReferenceList<T> parent,
            int fromIndex,
            int toIndex
    ) {
        super(uncheckedCast(parent), StageAdapter.identity());

        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    private ReferenceList(
            @NotNull ReferenceList<T> parent,
            @NotNull Comparator<KeyedReference<@NotNull Integer, T>> comparator
    ) {
        this(uncheckedCast(parent), StageAdapter.identity(), comparator);
    }

    public static <T> ReferenceList<T> create() {
        return new ReferenceList<>();
    }

    public static <T> ReferenceList<T> of(Collection<T> list) {
        ReferenceList<T> refs = create();
        refs.addAll(list);
        return refs;
    }

    public static <T> ReferenceList<T> empty() {
        //noinspection unchecked
        return (ReferenceList<T>) EMPTY;
    }

    @SafeVarargs
    public static <T> ReferenceList<T> of(T... values) {
        return of(Arrays.asList(values));
    }

    @Override
    protected final @Nullable Integer prefabBaseKey(@NotNull Integer key) {
        if (fromIndex == -1 & toIndex == -1)
            return key;
        int yield = fromIndex + key;
        if (yield > toIndex)
            return yield;
        return null;
    }

    /*
        public <X> ReferenceIndex<X, T> bi(Function<T, X> source) {
            return new KeyedPipe<>(this, BiStageAdapter.source(source), autoEmptyLimit);
        }
    */

    public final <R> ReferenceList<R> addStage(StageAdapter<T, R> stage) {
        ReferenceList<R> sub = new ReferenceList<>(this, stage);
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
    public final ReferenceList<T> subList(int fromIndex, int toIndex) {
        return new ReferenceList<>(this, fromIndex, toIndex);
    }

    public boolean addReference(KeyedReference<@NotNull Integer, T> ref) {
        return putAccessor(ref.getKey(), ref);
    }

    public final ReferenceList<T> filter(Predicate<? super T> predicate) {
        return addStage(StageAdapter.filter(predicate));
    }

    public final ReferenceList<T> yield(Predicate<? super T> predicate, Consumer<? super T> elseConsume) {
        return filter(it -> {
            if (predicate.test(it))
                return true;
            elseConsume.accept(it);
            return false;
        });
    }

    public final <R> ReferenceList<R> map(Function<? super T, ? extends R> mapper) {
        return addStage(StageAdapter.map(mapper));
    }

    public final <R> ReferenceList<R> flatMap(Class<R> target) {
        return filter(target::isInstance).map(target::cast);
    }

    public final <R> ReferenceList<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return addStage(StageAdapter.flatMap(mapper));
    }

    public final ReferenceList<T> peek(Consumer<? super T> action) {
        return addStage(StageAdapter.peek(action));
    }

    public final void forEach(Consumer<? super T> action) {
        //noinspection SimplifyStreamApiCallChains
        stream().forEach(action);
    }

    @Deprecated // todo: fix
    public final ReferenceList<T> distinct() {
        return addStage(StageAdapter.distinct());
    }

    @Deprecated // todo: fix
    public final ReferenceList<T> limit(long maxSize) {
        return addStage(StageAdapter.limit(maxSize));
    }

    @Deprecated // todo: fix
    public final ReferenceList<T> skip(long skip) {
        return addStage(StageAdapter.skip(skip));
    }

    public final ReferenceList<T> sorted() {
        return sorted(uncheckedCast(Comparator.naturalOrder()));
    }

    public final ReferenceList<T> sorted(Comparator<? super T> comparator) {
        return new ReferenceList<>(this, ReferenceAtlas.wrapComparator(comparator));
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
        return streamValues();
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
        addReference(KeyedReference.createKey(index, element));
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
        class OnceCompletingStage extends StageAdapter<T, T> {
            private final CompletableFuture<T> future = new CompletableFuture<>();

            protected OnceCompletingStage() {
                super(true, Function.identity());
            }

            @Override
            public KeyedReference<@NotNull Integer, T> advance(KeyedReference<@NotNull Integer, T> ref) {
                if (!ref.isNull() && !future.isDone()) {
                    future.complete(ref.get());
                    return ref;
                }
                return null;
            }
        }

        final OnceCompletingStage stage = new OnceCompletingStage();
        final ReferenceList<T> resulting = addStage(stage);
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
            index.consume(ReferenceList.this::remove);
        }

        @Override
        public void set(T t) {
            if (!index.test(i -> ReferenceList.this.getReference(i).set(t)))
                throw new UnsupportedOperationException("Unable to set");
        }

        @Override
        public void add(T t) {
            index.ifPresent(i -> ReferenceList.this.add(i, t));
        }
    }
}
