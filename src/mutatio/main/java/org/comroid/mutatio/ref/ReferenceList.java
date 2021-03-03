package org.comroid.mutatio.ref;

import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.adapter.StageAdapter;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.RefList;
import org.comroid.mutatio.model.RefOPs;
import org.comroid.mutatio.span.Span;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.uncheckedCast;

public class ReferenceList<T>
        extends ReferenceAtlas.ForList<Object, T>
        implements UncheckedCloseable, RefList<T> {
    private static final ReferenceList<Void> EMPTY = new ReferenceList<Void>() {{
        setImmutable();
    }};
    private final int fromIndex;
    private final int toIndex;

    public ReferenceList() {
        this(null);
    }

    @SuppressWarnings("CopyConstructorMissesField") // false positive
    public ReferenceList(
            @Nullable RefContainer<?, T> parent
    ) {
        this(uncheckedCast(parent), StageAdapter.identity());
    }

    public <In> ReferenceList(
            @Nullable RefContainer<?, In> parent,
            @NotNull StageAdapter<In, T> advancer
    ) {
        this(parent, advancer, null);
    }

    public <In> ReferenceList(
            @Nullable RefContainer<?, In> parent,
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

    @Override
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

    @Override
    public boolean addReference(KeyedReference<@NotNull Integer, T> ref) {
        return putAccessor(ref.getKey(), ref);
    }

    @Override
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

    @Override
    public final Stream<T> stream() {
        return streamValues();
    }

    @Override
    @Nullable
    public final T get(int index) {
        return getReference(index).get();
    }

    @Override
    public final Optional<T> wrap(int index) {
        return getReference(index).wrap();
    }

    @Override
    public final @NotNull T requireNonNull(int index) throws NullPointerException {
        return getReference(index).requireNonNull();
    }

    @Override
    public final @NotNull T requireNonNull(int index, String message) throws NullPointerException {
        return getReference(index).requireNonNull(message);
    }

    @Override
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

    @Override
    @Deprecated
    public Span<T> span() {
        return new Span<>(this, Span.DefaultModifyPolicy.SKIP_NULLS);
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
