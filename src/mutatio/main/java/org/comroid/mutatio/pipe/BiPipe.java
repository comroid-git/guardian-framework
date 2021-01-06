package org.comroid.mutatio.pipe;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;

import java.util.Comparator;
import java.util.function.*;

// todo
public interface BiPipe<X, Y> extends Pipe<Y> {
    @Override
    <R> BiPipe<X, R> addStage(StageAdapter<Y, R, Reference<Y>, Reference<R>> stage);

    <R> BiPipe<R, Y> addKeyStage(Function<X, R> keyMapper);

    default BiPipe<X, Y> filterKey(Predicate<? super Y> predicate) {
        return null;
    }

    @Override
    default BiPipe<X, Y> filter(Predicate<? super Y> predicate) {
        return null;
    }

    default <R> BiPipe<R, Y> mapKey(Function<? super X, ? extends R> mapper) {
        return null;
    }

    @Override
    default <R> BiPipe<X, R> map(Function<? super Y, ? extends R> mapper) {
        return null;
    }

    default <R> BiPipe<R, Y> flatMapKey(final Class<R> target) {
        return null;
    }

    @Override
    default <R> BiPipe<X, R> flatMap(final Class<R> target) {
        return null;
    }

    default <R> BiPipe<R, Y> flatMapKey(Function<? super X, ? extends Rewrapper<? extends Y>> mapper) {
        return null;
    }

    @Override
    default <R> BiPipe<X, R> flatMap(Function<? super Y, ? extends Rewrapper<? extends R>> mapper) {
        return null;
    }

    @Override
    default BiPipe<X, Y> peek(Consumer<? super Y> action) {
        return null;
    }

    default BiPipe<X, Y> peek(BiConsumer<? super X, ? super Y> action) {
        return null;
    }

    default ReferenceMap<X, Y> distinctKeys() {
        return null;
    }

    @Override
    default BiPipe<X, Y> distinct() {
        return addStage(StageAdapter.distinct());
    }

    default <R> Pipe<R> merge(BiFunction<? super X, ? super Y, ? extends R> merger) {
        return null;
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
}
