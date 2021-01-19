package org.comroid.mutatio.cache;

import org.comroid.api.Rewrapper;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.System.nanoTime;

public interface SingleValueCache<T> {
    Rewrapper<? extends SingleValueCache<?>> getParent();

    /**
     * @return Whether {@link #outdateCache()} was called on this container and it hasn't been {@linkplain #putIntoCache(Object) updated} yet.
     */
    boolean isOutdated();

    default boolean isUpToDate() {
        return !isOutdated();
    }

    @Internal
    Collection<? extends SingleValueCache<?>> getDependents();

    void cleanupDependents();

    T putIntoCache(T withValue);

    T getFromCache();

    /**
     * @return Whether the reference became outdated with this call.
     */
    boolean outdateCache();

    default ValueUpdateListener<T> onChange(Consumer<T> consumer) {
        return ValueUpdateListener.ofConsumer(this, consumer);
    }

    @Internal
    boolean outdateChildren();

    @Internal
    boolean addDependent(SingleValueCache<?> dependency);

    @Internal
    boolean attach(ValueUpdateListener<T> listener);

    @Internal
    boolean detach(ValueUpdateListener<T> listener);

    abstract class Abstract<T> implements SingleValueCache<T> {
        protected final Set<WeakReference<SingleValueCache<?>>> dependents = new HashSet<>();
        private final SingleValueCache.Abstract<?> parent;
        private final Set<ValueUpdateListener<T>> listeners = new HashSet<>();
        private final AtomicLong lastUpdate = new AtomicLong(0);
        private final AtomicReference<T> cache = new AtomicReference<>();

        @Override
        public Rewrapper<? extends SingleValueCache<?>> getParent() {
            return () -> parent;
        }

        @Override
        public final boolean isUpToDate() {
            return !isOutdated();
        }

        @Override
        public final synchronized boolean isOutdated() {
            return parent != null && lastUpdate.get() < parent.lastUpdate.get();
        }

        @Override
        public final boolean outdateChildren() {
            int c = 0;
            final Collection<? extends SingleValueCache<?>> dependent = getDependents();
            for (SingleValueCache<?> singleValueCache : dependent)
                if (singleValueCache != null && singleValueCache.outdateCache()) c++;
            return c == dependent.size();
        }

        @Override
        public final Collection<? extends SingleValueCache<?>> getDependents() {
            cleanupDependents();

            return dependents.stream()
                    .map(WeakReference::get)
                    .collect(Collectors.toSet());
        }

        protected Abstract(@Nullable SingleValueCache<?> parent) {
            try {
                this.parent = (Abstract<?>) parent;
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("Invalid parent; must implement CachedValue.Abstract: " + parent, cce);
            }

            if (parent != null && !parent.addDependent(this))
                throw new RuntimeException("Could not add new dependency to parent " + parent);
        }

        @Override
        public final void cleanupDependents() {
            dependents.removeIf(ref -> ref.get() == null);
        }

        @Override
        public final synchronized T putIntoCache(T withValue) {
            final long time = nanoTime();
            lastUpdate.set(time);
            cache.set(withValue);
            outdateChildren();
            listeners.forEach(listener -> listener.acceptNewValue(withValue));
            return withValue;
        }

        @Override
        public final T getFromCache() {
            return cache.get();
        }

        @Override
        public final boolean outdateCache() {
            if (isOutdated())
                return false;

            lastUpdate.set(0);
            outdateChildren();
            return true; // todo inspect
        }

        @Override
        public final boolean addDependent(SingleValueCache<?> dependency) {
            return dependents.add(new WeakReference<>(dependency));
        }

        @Override
        public final boolean attach(ValueUpdateListener<T> listener) {
            return listeners.add(listener);
        }

        @Override
        public final boolean detach(ValueUpdateListener<T> listener) {
            return listeners.remove(listener);
        }

        @Override
        public String toString() {
            return String.format("AbstractCachedValue{lastUpdate=%s}", lastUpdate.get());
        }
    }
}
