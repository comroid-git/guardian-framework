package org.comroid.mutatio.cache;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;

public interface SingleValueCache<T> extends ValueCache<T> {
    /**
     * Updates this cache with the new value and {@linkplain #deployListeners(Object) causes a ValueUpdate Event}.
     *
     * @param withValue The new value.
     * @return The new Value.
     */
    @Contract("_ -> param1")
    T putIntoCache(T withValue);

    T getFromCache();

    abstract class Abstract<T> extends ValueCache.Abstract<T> implements SingleValueCache<T> {
        private final AtomicReference<T> cache = new AtomicReference<>();

        protected Abstract(@Nullable SingleValueCache<?> parent) {
            super(parent);
        }

        @Override
        public final synchronized T putIntoCache(T withValue) {
            final long time = nanoTime();
            cache.set(withValue);
            updateCache();
            deployListeners(withValue);
            return withValue;
        }

        @Override
        public final T getFromCache() {
            return cache.get();
        }
    }
}
