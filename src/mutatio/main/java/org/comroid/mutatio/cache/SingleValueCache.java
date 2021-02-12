package org.comroid.mutatio.cache;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public interface SingleValueCache<T> extends ValueCache<T> {
    T getFromCache();

    boolean isAutoCompute();

    /**
     * Updates this cache with the new value and {@linkplain #deployListeners(Object) causes a ValueUpdate Event}.
     *
     * @param withValue The new value.
     * @return The new Value.
     */
    @Contract("_ -> param1")
    T putIntoCache(T withValue);

    @Internal
    T computeValue();

    abstract class Abstract<T> extends ValueCache.Abstract<T> implements SingleValueCache<T> {
        private final AtomicReference<T> cache = new AtomicReference<>();
        private final @Nullable Executor autocomputor;

        @Override
        public final T getFromCache() {
            return cache.get();
        }

        @Override
        public boolean isAutoCompute() {
            return autocomputor != null;
        }

        protected Abstract(@Nullable SingleValueCache<?> parent) {
            this(parent, null);
        }

        protected Abstract(@Nullable SingleValueCache<?> parent, @Nullable Executor autocomputor) {
            super(parent);

            this.autocomputor = autocomputor;
        }

        @Override
        public final synchronized T putIntoCache(T withValue) {
            cache.set(withValue);
            updateCache();
            if (autocomputor == null)
                deployListeners(withValue);
            else {
                autocomputor.execute(() -> {
                    deployListeners(withValue);
                    getDependents().stream()
                            .filter(SingleValueCache.class::isInstance)
                            .map(SingleValueCache.class::cast)
                            .forEach(SingleValueCache::computeValue);
                });
            }
            return withValue;
        }
    }
}
