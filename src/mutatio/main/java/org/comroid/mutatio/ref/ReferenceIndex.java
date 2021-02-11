package org.comroid.mutatio.ref;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.api.ThrowingRunnable;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.BiPipe;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.pipe.StageAdapter;
import org.comroid.mutatio.pipe.impl.BasicPipe;
import org.comroid.mutatio.pipe.impl.SortedResultingPipe;
import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.span.Span;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface ReferenceIndex<T> extends Pipeable<T>, ValueCache<Void>, Closeable {
    default boolean addReference(Reference<T> in) {
        throw new UnsupportedOperationException("Please add the Reference to the Pipe's base");
    }

    StageAdapter<?, T, Reference<?>, Reference<T>> getAdapter();

    default boolean isSorted() {
        return false;
    }

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

    static <T> Pipe<T> create() {
        return new BasicPipe<>(ReferenceIndex.create());
    }

    @SafeVarargs
    static <T> Pipe<T> of(T... values) {
        return of(Arrays.asList(values));
    }

    static <T> Pipe<T> of(Collection<T> collection) {
        final Pipe<T> pipe = create();
        collection.forEach(pipe::add);

        return pipe;
    }

    static <T> Pipe<T> ofStream(Stream<T> stream) {
        final Pipe<T> pipe = create();
        stream.iterator().forEachRemaining(pipe::add);
        return pipe;
    }

    static <T> Collector<Pump<T>, List<Pump<T>>, Pipe<T>> resultingPipeCollector(Executor executor) {
        class ResultingPipeCollector implements Collector<Pump<T>, List<Pump<T>>, Pipe<T>> {
            private final Pump<T> yield = Pump.create(executor);
            private final Supplier<List<Pump<T>>> supplier = ArrayList::new;
            private final BiConsumer<List<Pump<T>>, Pump<T>> accumulator = List::add;
            private final BinaryOperator<List<Pump<T>>> combiner = (l, r) -> {
                l.addAll(r);
                return l;
            };
            private final Function<List<Pump<T>>, Pipe<T>> finisher = pipes -> {
                pipes.forEach(pump -> pump
                        .map(Reference::constant)
                        .map(ref -> ref.map(Object.class::cast))
                        .peek(yield));
                return yield;
            };

            @Override
            public Supplier<List<Pump<T>>> supplier() {
                return supplier;
            }

            @Override
            public BiConsumer<List<Pump<T>>, Pump<T>> accumulator() {
                return accumulator;
            }

            @Override
            public BinaryOperator<List<Pump<T>>> combiner() {
                return combiner;
            }

            @Override
            public Function<List<Pump<T>>, Pipe<T>> finisher() {
                return finisher;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.singleton(Characteristics.IDENTITY_FINISH);
            }
        }

        return new ResultingPipeCollector();
    }

    default List<T> unwrap() {
        return span().unwrap();
    }

    int size();

    boolean add(T item);

    @OverrideOnly
    default boolean addReference(Reference<T> in) {
        return false;
    }

    <R> Pipe<R> addStage(StageAdapter<T, R, Reference<T>, Reference<R>> stage);

    default Pipe<T> filter(Predicate<? super T> predicate) {
        return addStage(StageAdapter.filter(predicate));
    }

    default Pipe<T> yield(Predicate<? super T> predicate, Consumer<T> elseConsume) {
        return filter(it -> {
            if (predicate.test(it))
                return true;
            elseConsume.accept(it);
            return false;
        });
    }

    @Deprecated
    default <R> Pipe<R> map(Class<R> target) {
        return flatMap(target);
    }

    default <R> Pipe<R> map(Function<? super T, ? extends R> mapper) {
        return addStage(StageAdapter.map(mapper));
    }

    default <R> Pipe<R> flatMap(Class<R> target) {
        return filter(target::isInstance).map(target::cast);
    }

    default <R> Pipe<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return addStage(StageAdapter.flatMap(mapper));
    }

    default Pipe<T> peek(Consumer<? super T> action) {
        return addStage(StageAdapter.peek(action));
    }

    default void forEach(Consumer<? super T> action) {
        addStage(StageAdapter.peek(action)).unwrap();
    }

    @Deprecated // todo: fix
    default Pipe<T> distinct() {
        return addStage(StageAdapter.distinct());
    }

    @Deprecated // todo: fix
    default Pipe<T> limit(long maxSize) {
        return addStage(StageAdapter.limit(maxSize));
    }

    @Deprecated // todo: fix
    default Pipe<T> skip(long skip) {
        return addStage(StageAdapter.skip(skip));
    }

    default Pipe<T> sorted() {
        return sorted(Polyfill.uncheckedCast(Comparator.naturalOrder()));
    }

    default Pipe<T> sorted(Comparator<? super T> comparator) {
        return new SortedResultingPipe<>(this, comparator);
    }

    @NotNull
    default Reference<T> findFirst() {
        return sorted().findAny();
    }

    @NotNull
    default Reference<T> findAny() {
        return Reference.conditional(() -> size() > 0, () -> get(0));
    }

    default boolean remove(T item) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("remove() is not supported by pipe");
    }

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
        Reference<T> ref = getReference(index);
        if (ref == null)
            return null;
        return ref.get();
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

    <X> BiPipe<X, T> bi(Function<T, X> mapper);

    default Span<T> span() {
        return new Span<>(this, Span.DefaultModifyPolicy.SKIP_NULLS);
    }

    default CompletableFuture<T> next() {
        class OnceCompletingStage implements StageAdapter<T, T,Reference<T>,Reference<T>> {
            private final CompletableFuture<T> future = new CompletableFuture<>();

            @Override
            public Reference<T> advance(Reference<T> ref) {
                if (!ref.isNull() && !future.isDone())
                    future.complete(ref.get());
                return Reference.empty();
            }
        }

        final OnceCompletingStage stage = new OnceCompletingStage();
        final Pipe<T> resulting = addStage(stage);
        stage.future.thenRun(ThrowingRunnable.handling(resulting::close, null));

        return stage.future;
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
                list.add(Reference.constant(item));
                updateCache();
                return true;
            }

            @Override
            public boolean addReference(Reference<T> in) {
                list.add(in);
                updateCache();
                return true;
            }

            @Override
            public boolean remove(T item) {
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
            public Pipe<T> pipe() {
                return ReferenceIndex.of(list).flatMap(Function.identity());
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
