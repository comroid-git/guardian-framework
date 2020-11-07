package org.comroid.mutatio.pipe;

import org.comroid.mutatio.ref.Reference;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface MapPipe<K, V> extends BiPipe<K, V> {
    @Override
    <R> MapPipe<K, R> addStage(StageAdapter<V, R> stage);

    @Override
    default Pipe<V> filter(Predicate<? super V> predicate) {
        return null;
    }

    @Override
    default <R> Pipe<R> map(Function<? super V, ? extends R> mapper) {
        return null;
    }

    @Override
    default <R> Pipe<R> flatMap(final Class<R> target) {
        return null;
    }

    @Override
    default <R> Pipe<R> flatMap(Function<? super V, ? extends Reference<? extends R>> mapper) {
        return null;
    }

    @Override
    default Pipe<V> peek(Consumer<? super V> action) {
        return null;
    }

    @Override
    <X> BiPipe<V, X, V, X> bi(Function<V, X> mapper);
}
