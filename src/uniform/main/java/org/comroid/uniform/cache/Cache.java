package org.comroid.uniform.cache;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.model.RefMap;

import java.util.concurrent.CompletableFuture;

public interface Cache<K, V> extends RefMap<K, V>, ContextualProvider.Underlying {
    default boolean canProvide() {
        return false;
    }

    default CompletableFuture<V> provide(K key) {
        return Polyfill.failedFuture(new UnsupportedOperationException("Cache can't provide!"));
    }
}
