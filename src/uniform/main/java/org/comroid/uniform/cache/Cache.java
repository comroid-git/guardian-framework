package org.comroid.uniform.cache;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.model.RefMap;
import org.comroid.mutatio.ref.Reference;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface Cache<K, V> extends Iterable<CacheReference<K, V>>, RefMap<K, V>, ContextualProvider.Underlying {
    <R> Reference<R> accessor(K key, String name, Reference.Advancer<V, ? extends R> advancer);

    default boolean canProvide() {
        return false;
    }

    default CompletableFuture<V> provide(K key) {
        return Polyfill.failedFuture(new UnsupportedOperationException("Cache can't provide!"));
    }

    @Override
    default void forEach(BiConsumer<? super K, ? super V> action) {
        forEach(entry -> action.accept(entry.getKey(), entry.getValue()));
    }
}
