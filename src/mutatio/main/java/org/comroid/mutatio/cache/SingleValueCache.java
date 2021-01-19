package org.comroid.mutatio.cache;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;

public interface SingleValueCache<T> extends ValueCache<T> {
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
            lastUpdate.set(time);
            cache.set(withValue);
            outdateDependents();
            listeners.forEach(listener -> listener.acceptNewValue(withValue));
            return withValue;
        }

        @Override
        public final T getFromCache() {
            return cache.get();
        }

        @Override
        public String toString() {
            return String.format("AbstractCachedValue{lastUpdate=%s}", lastUpdate.get());
        }
    }
}
