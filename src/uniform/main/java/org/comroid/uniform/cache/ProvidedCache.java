package org.comroid.uniform.cache;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;

public class ProvidedCache<K, V> extends BasicCache<K, V> {
    public static final Executor DEFAULT_EXECUTOR = ForkJoinPool.commonPool();
    private final Executor providerWriteExecutor;
    private final Function<K, CompletableFuture<V>> valueProvider;

    public ProvidedCache(
            int largeThreshold, Executor providerWriteExecutor, Function<K, CompletableFuture<V>> valueProvider
    ) {
        super(largeThreshold);

        this.providerWriteExecutor = providerWriteExecutor;
        this.valueProvider = valueProvider;
    }

    @Override
    public boolean canProvide() {
        return true;
    }

    @Override
    public CompletableFuture<V> provide(K key) {
        if (!containsKey(key)) {
            CompletableFuture<V> future = valueProvider.apply(key);
            future.thenAcceptAsync(it -> getReference(key, true).set(it), providerWriteExecutor);
        }

        return getReference(key, false).provider().get();
    }
}
