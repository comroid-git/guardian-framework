package org.comroid.mutatio.ref;

import org.comroid.mutatio.pipe.BasicPipe;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.pump.BasicPump;
import org.comroid.mutatio.pump.Pump;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public interface ReferenceIndex<T> extends Pipeable<T> {
    static <T> ReferenceIndex<T> create() {
        return of(new ArrayList<>());
    }

    static <T> ReferenceIndex<T> of(List<T> list) {
        return new Support.OfList<>(list);
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

    boolean remove(T item);

    /**
     * Deletes all elements
     */
    void clear();

    default Stream<T> stream() {
        return unwrap().stream();
    }

    @Override
    default Pipe<T> pipe() {
        return new BasicPipe<>(this);
    }

    @Override
    default Pump<T> pump(Executor executor) {
        return new BasicPump<>(executor, this);
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

    final class Support {
        public static final ReferenceIndex<?> EMPTY = ReferenceIndex.of(Collections.emptyList());

        private static final class OfList<T> implements ReferenceIndex<T> {
            private final List<T> underlying;

            private OfList(List<T> underlying) {
                this.underlying = underlying;
            }

            @Override
            public boolean add(T item) {
                return underlying.add(item);
            }

            @Override
            public boolean remove(T item) {
                return underlying.remove(item);
            }

            @Override
            public void clear() {
                underlying.clear();
            }

            @Override
            public Pipe<T> pipe() {
                return Pipe.of(underlying);
            }

            @Override
            public Reference<T> getReference(final int index) {
                return Reference.conditional(
                        () -> index < 0 || underlying.size() >= index,
                        () -> underlying.get(index)
                );
            }

            @Override
            public List<T> unwrap() {
                return underlying;
            }

            @Override
            public int size() {
                return underlying.size();
            }
        }
    }
}
