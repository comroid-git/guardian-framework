package org.comroid.mutatio.cache;

import org.comroid.annotations.inheritance.MustExtend;
import org.comroid.api.Named;
import org.comroid.api.Rewrapper;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.nanoTime;

@MustExtend(ValueCache.Abstract.class)
public interface ValueCache<T> extends Named {
    Rewrapper<? extends ValueCache<?>> getParent();

    boolean isOutdated();

    void setOutdated(boolean state);

    boolean isUpToDate();

    @Internal
    long getLastUpdateTime();

    @Internal
    Collection<? extends ValueCache<?>> getDependents();

    @NonExtendable
    default ValueUpdateListener<T> onChange(Consumer<T> consumer) {
        return ValueUpdateListener.ofConsumer(this, consumer);
    }

    default Stream<? extends ValueCache<?>> upstream() {
        return Stream.concat(
                getParent().stream(),
                getParent().ifPresentMapOrElseGet(
                        ValueCache::upstream,
                        Stream::empty
                ));
    }

    default boolean dependsOn(ValueCache<?> other) {
        return upstream().anyMatch(other::equals);
    }

    /**
     * Marks this cache as updated now, but does not {@linkplain #deployListeners(Object) cause a ValueUpdate Event}.
     * Implicitly calls {@link #outdateDependents()}.
     * Bulk operations may choose to not mark each change individually.
     */
    @Internal
    void updateCache();

    /**
     * Marks this cache as outdated, but does not {@linkplain #deployListeners(Object) cause a ValueUpdate Event}.
     * Implicitly calls {@link #outdateDependents()}.
     * Bulk operations may choose to not mark each change individually.
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

    boolean removeDependent(ValueCache<?> dependent);

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

    interface Underlying<T> extends ValueCache<T> {
        ValueCache<T> getUnderlyingValueCache();

        @Override
        default Rewrapper<? extends ValueCache<?>> getParent() {
            return getUnderlyingValueCache().getParent();
        }

        @Override
        default boolean isOutdated() {
            return getUnderlyingValueCache().isOutdated();
        }

        @Override
        default boolean isUpToDate() {
            return getUnderlyingValueCache().isUpToDate();
        }

        @Override
        default Collection<? extends ValueCache<?>> getDependents() {
            return getUnderlyingValueCache().getDependents();
        }

        @Override
        default void updateCache() {
            getUnderlyingValueCache().updateCache();
        }

        @Override
        default void outdateCache() {
            getUnderlyingValueCache().outdateCache();
        }

        @Override
        default boolean addDependent(ValueCache<?> dependency) {
            return getUnderlyingValueCache().addDependent(dependency);
        }

        @Override
        default boolean attach(ValueUpdateListener<T> listener) {
            return getUnderlyingValueCache().attach(listener);
        }

        @Override
        default boolean detach(ValueUpdateListener<T> listener) {
            return getUnderlyingValueCache().detach(listener);
        }

        @Override
        default int deployListeners(T forValue, Executor executor) {
            return getUnderlyingValueCache().deployListeners(forValue, executor);
        }
    }

    abstract class Abstract<T, P extends ValueCache<?>> implements ValueCache<T> {
        protected final @Nullable P parent;
        private final Set<WeakReference<? extends ValueCache<?>>> dependents = new HashSet<>();
        private final AtomicLong lastUpdate = new AtomicLong(0);
        private final Set<ValueUpdateListener<T>> listeners = new HashSet<>();

        @Override
        public Rewrapper<? extends ValueCache<?>> getParent() {
            return () -> parent;
        }

        @Override
        public final boolean isUpToDate() {
            return !isOutdated();
        }

        @Override
        public final long getLastUpdateTime() {
            return lastUpdate.get();
        }

        @Override
        public final synchronized boolean isOutdated() {
            if (parent != null)
                return parent.getLastUpdateTime() >= lastUpdate.get();
            return lastUpdate.get() == 0;
        }

        @Override
        public final void setOutdated(boolean state) {
            lastUpdate.set(state ? Long.MAX_VALUE : Long.MIN_VALUE);
        }

        @Override
        public final Collection<? extends ValueCache<?>> getDependents() {
            return dependents.stream()
                    .filter(Objects::nonNull)
                    .map(WeakReference::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        protected Abstract(P parent) {
            try {
                this.parent = parent;
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("Invalid parent; must implement ValueCache.Abstract: " + parent, cce);
            }

            if (parent != null && !parent.addDependent(this))
                throw new RuntimeException("Could not add new dependency to parent " + parent);
        }

        @Override
        public final boolean addDependent(ValueCache<?> dependent) {
            return dependents.add(new WeakReference<>(dependent));
        }

        @Override
        public final boolean removeDependent(ValueCache<?> dependent) {
            for (WeakReference<? extends ValueCache<?>> ref : dependents) {
                ValueCache<?> v = ref.get();
                if (v == null || v.equals(dependent))
                    return dependents.remove(ref);
            }
            return false;
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
            listeners.forEach(listener -> executor.execute(() -> listener.acceptNewValue(forValue)));
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
}
