package org.comroid.uniform.cache;

import org.comroid.api.Polyfill;
import org.comroid.api.Provider;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.stack.MutableStack;
import org.comroid.mutatio.stack.RefStack;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class CacheReference<K, V> extends KeyedReference<K, V> {
    public final AtomicReference<V> reference = new AtomicReference<>(null);
    private final org.comroid.mutatio.ref.Reference<CompletableFuture<V>> firstValueFuture = Reference.create();
    private final Object lock = Polyfill.selfawareObject();

    {
        adjustStackSize(1);
        stack()[0] = new Accessor(null);
    }

    public CacheReference(K key) {
        super(key, true);

        this.firstValueFuture.putIntoCache(new CompletableFuture<>());
    }

    public CacheReference(K key, V initValue) {
        super(key, true);

        this.firstValueFuture.outdateCache();
    }

    public static <K, V> CacheReference<K, V> createCache() {
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
    public Provider<V> provider() {
        if (firstValueFuture.isOutdated()) {
            firstValueFuture.outdateCache();
            return Provider.of(firstValueFuture.get());
        }

        return Provider.of(this);
    }

    private class Accessor extends MutableStack<V> {
        @Override
        public boolean isMutable() {
            return true;
        }

        protected Accessor(@Nullable RefStack<?> parent) {
            super(parent, "CacheReference#Accessor");
        }

        @Nullable
        @Override
        protected V $get() {
            synchronized (lock) {
                return reference.get();
            }
        }

        @Override
        protected boolean $set(V value) {
            synchronized (lock) {
                if (!firstValueFuture.isOutdated()
                        && !firstValueFuture.isNull()
                        && !Objects.requireNonNull(firstValueFuture.get(), "AssertionFailure").isDone()) {
                    firstValueFuture.requireNonNull().complete(value);
                }

                return reference.getAndSet(value) != value;
            }
        }
    }
}
