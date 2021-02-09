package org.comroid.mutatio.pipe;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.api.ThrowingRunnable;
import org.comroid.mutatio.pipe.impl.BasicPipe;
import org.comroid.mutatio.pipe.impl.SortedResultingPipe;
import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.span.Span;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

public interface Pipe<T> extends ReferenceIndex<T>, Closeable {
    @Override
    default boolean addReference(Reference<T> in) {
        throw new UnsupportedOperationException("Please add the Reference to the Pipe's base");
    }

    StageAdapter<?, T, Reference<?>, Reference<T>> getAdapter();

    default boolean isSorted() {
        return false;
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

    @Override
    default List<T> unwrap() {
        return span().unwrap();
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

    default <R> Pipe<R> flatMap(final Class<R> target) {
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

    @Override
    default boolean remove(T item) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("remove() is not supported by pipe");
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
}
