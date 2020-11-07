package org.comroid.mutatio.pipe;

import org.comroid.mutatio.ref.Reference;

import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface BiPipe<X, Y> extends Pipe<Y> {
    @Override
    <R> BiPipe<X, R> addStage(StageAdapter<Y, R> stage);

    <R> BiPipe<R, Y> addKeyStage(Function<X, R> keyMapper);

    @Override
    default BiPipe<X, Y> filter(Predicate<? super Y> predicate) {
        return null;
    }

    @Override
    default <R> BiPipe<X, R> map(Function<? super Y, ? extends R> mapper) {
        return null;
    }

    @Override
    default <R> BiPipe<X, R> flatMap(final Class<R> target) {
        return null;
    }

    @Override
    default <R> BiPipe<X, R> flatMap(Function<? super Y, ? extends Reference<? extends R>> mapper) {
        return null;
    }

    @Override
    default BiPipe<X, Y> peek(Consumer<? super Y> action) {
        return null;
    }

    @Override
    default BiPipe<X, Y> distinct() {
        return addStage(StageAdapter.distinct());
    }

    @Override
    default BiPipe<X, Y> limit(long maxSize) {
        return addStage(StageAdapter.limit(maxSize));
    }

    @Override
    default BiPipe<X, Y> skip(long skip) {
        return null;
    }

    @Override
    default Pipe<Y> sorted() {
        return null;
    }

    @Override
    default Pipe<Y> sorted(Comparator<? super Y> comparator) {
        return null;
    }

    @Override
    <R> BiPipe<Y, R> bi(Function<Y, R> mapper);
}
