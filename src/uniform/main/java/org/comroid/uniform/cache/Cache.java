package org.comroid.uniform.cache;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.ref.ReferenceMap;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface Cache<K, V> extends Iterable<CacheReference<K, V>>, ReferenceMap<K, V> {
    <R> Processor<R> accessor(K key, String name, Processor.Advancer<V, ? extends R> advancer);

    @Override
    default Pipe<V> pipe() {
        return pipe(any -> true).map(org.comroid.mutatio.ref.Reference::get);
    }

    default boolean canProvide() {
        return false;
    }

    default CompletableFuture<V> provide(K key) {
        return Polyfill.failedFuture(new UnsupportedOperationException("Cache can't provide!"));
    }

    @Override
    default void forEach(BiConsumer<K, V> action) {
        forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }
}
