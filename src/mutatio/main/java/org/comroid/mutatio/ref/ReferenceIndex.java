package org.comroid.mutatio.ref;

import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.pipe.StageAdapter;
import org.comroid.mutatio.pipe.impl.BasicPipe;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ReferenceIndex<T> extends Pipeable<T>, ValueCache<Void> {
    static <T> ReferenceIndex<T> create() {
        return of(new ArrayList<>());
    }

    static <T> ReferenceIndex<T> of(List<T> list) {
        Support.OfList<T> index = new Support.OfList<>();
        list.forEach(index::add);
        return index;
    }

    static <T> ReferenceIndex<T> empty() {
        //noinspection unchecked
        return (ReferenceIndex<T>) Support.EMPTY;
    }

    default ReferenceIndex<T> subset() {
        return subset(0, size());
    }

    default ReferenceIndex<T> subset(int startIncl, int endExcl) {
        final ReferenceIndex<T> subset = create();

        for (int i = startIncl; i < endExcl; i++)
            subset.add(get(i));

        return subset;
    }

    //todo: returns empty list
    List<T> unwrap();

    int size();

    boolean add(T item);

    @OverrideOnly
    default boolean addReference(Reference<T> in) {
        return false;
    }

    boolean remove(T item);

    /**
     * Deletes all elements
     */
    void clear();

    Stream<? extends Reference<T>> streamRefs();

    default Stream<T> stream() {
        return unwrap().stream();
    }

    @Override
    default Pipe<T> pipe() {
        return new BasicPipe<>(this);
    }

    Reference<T> getReference(int index);

    @Nullable
    default T get(int index) {
        return Objects.requireNonNull(getReference(index), "AssertionFailure: Reference is null").get();
    }

    default Optional<T> wrap(int index) {
        return getReference(index).wrap();
    }

    default @NotNull T requireNonNull(int index) throws NullPointerException {
        return getReference(index).requireNonNull();
    }

    default @NotNull T requireNonNull(int index, String message) throws NullPointerException {
        return getReference(index).requireNonNull(message);
    }

    @Internal
    @Experimental
    default <
            Out,
            InRef extends Reference<T>,
            OutRef extends Reference<Out>,
            Count,
            Adp extends StageAdapter<T, Out, InRef, OutRef>
            > void generateAccessors(
            final Map<Integer, OutRef> accessors,
            final Adp adapter,
            final BiFunction<Adp, InRef, OutRef> refAccumulator
    ) {
        for (int i = 0; i < size(); i++) {
            //noinspection unchecked
            InRef inRef = (InRef) getReference(i);
            OutRef outRef = refAccumulator.apply(adapter, inRef);
            accessors.put(i, outRef);
        }
    }

    final class Support {
        public static final ReferenceIndex<?> EMPTY = ReferenceIndex.of(Collections.emptyList());

        public static final class OfList<T> extends ValueCache.Abstract<Void> implements ReferenceIndex<T> {
            private final List<Reference<T>> list;

            private OfList() {
                this(new ArrayList<>());
            }

            private OfList(List<Reference<T>> list) {
                super(null);

                this.list = list;
            }

            @Override
            public boolean add(T item) {
                return list.add(Reference.constant(item)) && updateCache();
            }

            @Override
            public boolean addReference(Reference<T> in) {
                return list.add(in) && updateCache();
            }

            @Override
            public boolean remove(T item) {
                return list.removeIf(ref -> ref.contentEquals(item)) && updateCache();
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
            public Pipe<T> pipe() {
                return Pipe.of(list).flatMap(Function.identity());
            }

            @Override
            public Reference<T> getReference(final int index) {
                return list.get(index);
            }

            @Override
            public List<T> unwrap() {
                return Collections.unmodifiableList(list.stream()
                        .flatMap(Reference::stream)
                        .collect(Collectors.toList()));
            }

            @Override
            public int size() {
                return list.size();
            }
        }
    }
}
