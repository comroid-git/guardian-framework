package org.comroid.mutatio.cache;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public interface SingleValueCache<T> extends ValueCache<T> {
    T getFromCache();

    default boolean isAutoCompute() {
        return getExecutor() != null;
    }

    @Nullable
    Executor getExecutor();

    /**
     * Updates this cache with the new value and {@linkplain #deployListeners(Object) causes a ValueUpdate Event}.
     *
     * @param withValue The new value.
     * @return The new Value.
     */
    @Contract("_ -> param1")
    T putIntoCache(T withValue);

    @Internal
    void computeAndStoreValue();

    abstract class Abstract<T> extends ValueCache.Abstract<T, ValueCache<?>> implements SingleValueCache<T> {
        private final AtomicReference<T> cache = new AtomicReference<>();
        private final @Nullable Executor executor;

        @Override
        public final T getFromCache() {
            return cache.get();
        }

        @Override
        public @Nullable Executor getExecutor() {
            return executor;
        }

        protected Abstract(@Nullable SingleValueCache<?> parent) {
            this(parent, parent != null ? parent.getExecutor() : null);
        }

        protected Abstract(@Nullable ValueCache<?> parent, @Nullable Executor autocomputor) {
            super(parent);

            this.executor = autocomputor;
        }

        @Override
        public final synchronized T putIntoCache(T withValue) {
            cache.set(withValue);
            updateCache();
            if (executor == null) fireListeners(withValue, false);
            else executor.execute(() -> fireListeners(withValue, true));
            return withValue;
        }

        private void fireListeners(T withValue, boolean doTransient) {
            deployListeners(withValue);
            if (doTransient) getDependents().stream()
                    .filter(SingleValueCache.class::isInstance)
                    .map(SingleValueCache.class::cast)
                    .forEach(SingleValueCache::computeAndStoreValue);
        }
    }
}
