package org.comroid.uniform.cache;

import org.comroid.api.Polyfill;
import org.comroid.api.Provider;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class CacheReference<K, V> extends KeyedReference<K, V> {
    public final AtomicReference<V> reference = new AtomicReference<>(null);
    private final org.comroid.mutatio.ref.Reference<CompletableFuture<V>> firstValueFuture = Reference.create();
    private final Object lock = Polyfill.selfawareObject();

    public CacheReference(K key) {
        super(key, true);

        this.firstValueFuture.putIntoCache(new CompletableFuture<>());
    }

    public CacheReference(K key, V initValue) {
        super(key, true);

        this.firstValueFuture.outdateCache();
    }

    public static <K, V> CacheReference<K, V> createCache() {
        //noinspection unchecked
        return new CacheReference<>(null);
    }

    public static <K, V> CacheReference<K, V> constant(K key, V value) {
        return new CacheReference<K, V>(key, value) {
            @Override
            public boolean isMutable() {
                return false;
            }
        };
    }

    @Override
    protected boolean doSet(V value) {
        synchronized (lock) {
            if (!firstValueFuture.isOutdated()
                    && !firstValueFuture.isNull()
                    && !Objects.requireNonNull(firstValueFuture.get(), "AssertionFailure").isDone()) {
                firstValueFuture.requireNonNull().complete(value);
            }

            return reference.getAndSet(value) != value;
        }
    }

    @Nullable
    @Override
    protected V doGet() {
        synchronized (lock) {
            return reference.get();
        }
    }

    @Override
    public Provider<V> provider() {
        if (firstValueFuture.isOutdated()) {
            firstValueFuture.outdateCache();
            return Provider.of(firstValueFuture.get());
        }

        return Provider.of(this);
    }
}
