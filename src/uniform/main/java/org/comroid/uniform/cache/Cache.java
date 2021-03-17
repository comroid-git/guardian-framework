package org.comroid.uniform.cache;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.model.RefMap;
import org.comroid.mutatio.ref.Reference;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

public interface Cache<K, V> extends RefMap<K, V>, ContextualProvider.Underlying {
    default boolean canProvide() {
        return false;
    }

    default CompletableFuture<V> provide(K key) {
        return Polyfill.failedFuture(new UnsupportedOperationException("Cache can't provide!"));
    }
}
