package org.comroid.mutatio.cache;

import org.comroid.annotations.inheritance.MustExtend;
import org.comroid.api.Rewrapper;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.System.nanoTime;

@MustExtend(ValueCache.Abstract.class)
public interface ValueCache<T> {
    Rewrapper<? extends ValueCache<?>> getParent();

    boolean isOutdated();

    boolean isUpToDate();

    @NonExtendable
    default ValueUpdateListener<T> onChange(Consumer<T> consumer) {
        return ValueUpdateListener.ofConsumer(this, consumer);
    }

    /**
     * Marks this cache as updated now, but does not {@linkplain #deployListeners(Object) cause a ValueUpdate Event}.
     * Implicitly calls {@link #outdateDependents()}.
     * Bulk operations may choose to not mark each change individually.
     *
     */
    @Internal
    void updateCache();

    /**
     * Marks this cache as outdated, but does not {@linkplain #deployListeners(Object) cause a ValueUpdate Event}.
     * Implicitly calls {@link #outdateDependents()}.
     * Bulk operations may choose to not mark each change individually.
     *
     */
    @Internal
    void outdateCache();

    /**
     * Marks all dependents as outdated, must be called when this cache is changed.
     */
    @Internal
    @NonExtendable
    default void outdateDependents() {
        getDependents().forEach(ValueCache::outdateCache);
    }

    @Internal
    boolean addDependent(ValueCache<?> dependency);

    @Internal
    Collection<? extends ValueCache<?>> getDependents();

    @Internal
    boolean attach(ValueUpdateListener<T> listener);

    @Internal
    boolean detach(ValueUpdateListener<T> listener);

    /**
     * Fires all attached {@link ValueUpdateListener}s with the given new value.
     *
     * @param forValue The new value.
     * @return How many listeners were fired.
     */
    @Internal
    default int deployListeners(T forValue) {
        return deployListeners(forValue, Runnable::run);
    }

    /**
     * Fires all attached {@link ValueUpdateListener}s with the given new value.
     *
     * @param forValue The new value.
     * @param executor The executor to deploy on.
     * @return How many listeners were fired.
     */
    @Internal
    int deployListeners(T forValue, Executor executor);

    abstract class Abstract<T> implements ValueCache<T> {
        private final Set<WeakReference<? extends ValueCache<?>>> dependents = new HashSet<>();
        private final AtomicLong lastUpdate = new AtomicLong(0);
        private final Set<ValueUpdateListener<T>> listeners = new HashSet<>();
        protected final ValueCache.Abstract<?> parent;

        @Override
        public Rewrapper<? extends ValueCache<?>> getParent() {
            return () -> parent;
        }

        @Override
        public final boolean isUpToDate() {
            return !isOutdated();
        }

        @Override
        public final synchronized boolean isOutdated() {
            if (parent != null)
                return parent.lastUpdate.get() >= lastUpdate.get();
            return lastUpdate.get() == 0;
        }

        protected Abstract(ValueCache<?> parent) {
            try {
                this.parent = (Abstract<?>) parent;
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("Invalid parent; must implement ValueCache.Abstract: " + parent, cce);
            }

            if (parent != null && !parent.addDependent(this))
                throw new RuntimeException("Could not add new dependency to parent " + parent);
        }

        @Override
        public final boolean addDependent(ValueCache<?> dependency) {
            return dependents.add(new WeakReference<>(dependency));
        }

        @Override
        public final Collection<? extends ValueCache<?>> getDependents() {
            dependents.removeIf(ref -> ref.get() == null);
            return dependents.stream()
                    .map(WeakReference::get)
                    .collect(Collectors.toSet());
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
        public final int deployListeners(final T forValue, Executor executor) {
            executor.execute(() -> listeners.forEach(listener -> listener.acceptNewValue(forValue)));
            return listeners.size();
        }

        @Override
        public final void updateCache() {
            lastUpdate.set(nanoTime());
            outdateDependents();
        }

        @Override
        public final void outdateCache() {
            lastUpdate.set(0);
            outdateDependents();
        }
    }

    class Underlying<T> implements ValueCache<T> {
        private final ValueCache<T> underlying;

        @Override
        public final Rewrapper<? extends ValueCache<?>> getParent() {
            return underlying.getParent();
        }

        @Override
        public final boolean isOutdated() {
            return underlying.isOutdated();
        }

        @Override
        public final boolean isUpToDate() {
            return underlying.isUpToDate();
        }

        protected Underlying(ValueCache<T> underlying) {
            this.underlying = underlying;
        }

        @Override
        public final void updateCache() {
            underlying.updateCache();
        }

        @Override
        public final void outdateCache() {
            underlying.outdateCache();
        }

        @Override
        public final boolean addDependent(ValueCache<?> dependency) {
            return underlying.addDependent(dependency);
        }

        @Override
        public final Collection<? extends ValueCache<?>> getDependents() {
            return underlying.getDependents();
        }

        @Override
        public final boolean attach(ValueUpdateListener<T> listener) {
            return underlying.attach(listener);
        }

        @Override
        public final boolean detach(ValueUpdateListener<T> listener) {
            return underlying.detach(listener);
        }

        @Override
        public int deployListeners(T forValue, Executor executor) {
            return underlying.deployListeners(forValue, executor);
        }
    }
}
