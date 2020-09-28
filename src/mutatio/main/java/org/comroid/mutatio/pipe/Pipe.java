package org.comroid.mutatio.pipe;

import org.comroid.api.Polyfill;
import org.comroid.api.ThrowingRunnable;
import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.pump.BasicPump;
import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.span.Span;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Stream;

@SuppressWarnings("TypeParameterExplicitlyExtendsObject")
public interface Pipe<O> extends ReferenceIndex<O>, Consumer<Reference<Object>>, AutoCloseable {
    StageAdapter<? extends Object, O> getAdapter();

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
        collection.stream()
                .map(Reference::constant)
                .map(ref -> ref.map(Object.class::cast))
                .forEach(pipe);

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
    default List<O> unwrap() {
        return span().unwrap();
    }

    <R> Pipe<R> addStage(StageAdapter<O, R> stage);

    default Pipe<O> filter(Predicate<? super O> predicate) {
        return addStage(StageAdapter.filter(predicate));
    }

    default <R> Pipe<R> map(Class<R> target) {
        return filter(target::isInstance).map(target::cast);
    }

    default <R> Pipe<R> map(Function<? super O, ? extends R> mapper) {
        return addStage(StageAdapter.map(mapper));
    }

    default <R> Pipe<R> flatMap(Function<? super O, ? extends Reference<? extends R>> mapper) {
        return addStage(StageAdapter.flatMap(mapper));
    }

    default Pipe<O> peek(Consumer<? super O> action) {
        return addStage(StageAdapter.peek(action));
    }

    default void forEach(Consumer<? super O> action) {
        addStage(StageAdapter.peek(action)).unwrap();
    }

    default Pipe<O> distinct() {
        return addStage(StageAdapter.distinct());
    }

    default Pipe<O> limit(long maxSize) {
        return addStage(StageAdapter.limit(maxSize));
    }

    default Pipe<O> skip(long skip) {
        return addStage(StageAdapter.skip(skip));
    }

    default Pipe<O> sorted() {
        return sorted(Polyfill.uncheckedCast(Comparator.naturalOrder()));
    }

    default Pipe<O> sorted(Comparator<? super O> comparator) {
        return new SortedResultingPipe<>(this, comparator);
    }

    @NotNull
    default Processor<O> findFirst() {
        return sorted().findAny();
    }

    @NotNull
    default Processor<O> findAny() {
        return Processor.ofReference(Reference.conditional(() -> size() > 0, () -> get(0)));
    }

    @Override
    default boolean remove(O item) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("remove() is not supported by pipe");
    }

    @Override
    default Pump<O> pump(Executor executor) {
        return new BasicPump<>(executor, this.map(Polyfill::uncheckedCast));
    }

    <X> BiPipe<O, X, O, X> bi(Function<O, X> mapper);

    /**
     * Only meant for use from a {@link Pump} instance.
     */
    @Override
    default void accept(Reference<Object> input) {
        throw new UnsupportedOperationException("Method #accept is only meant for use from a Pump instance");
    }

    default Span<O> span() {
        return new Span<>(this, Span.DefaultModifyPolicy.SKIP_NULLS);
    }

    default CompletableFuture<O> next() {
        class OnceCompletingStage implements StageAdapter<O, O> {
            private final CompletableFuture<O> future = new CompletableFuture<>();

            @Override
            public Reference<O> advance(Reference<O> ref) {
                if (!ref.isNull() && !future.isDone())
                    future.complete(ref.get());
                return Reference.empty();
            }
        }

        final OnceCompletingStage stage = new OnceCompletingStage();
        final Pipe<O> resulting = addStage(stage);
        stage.future.thenRun(ThrowingRunnable.handling(resulting::close, null));

        return stage.future;
    }
}
