package org.comroid.mutatio.cache;

import org.comroid.api.Rewrapper;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface ValueCache<T> {
    Rewrapper<? extends ValueCache<?>> getParent();

    boolean isOutdated();

    boolean isUpToDate();

    default ValueUpdateListener<T> onChange(Consumer<T> consumer) {
        return ValueUpdateListener.ofConsumer(this, consumer);
    }

    /**
     * @return Whether the reference became outdated with this call.
     */
    boolean outdateCache();

    @Internal
    boolean outdateDependents();

    @Internal
    boolean addDependent(ValueCache<?> dependency);

    @Internal
    void cleanupDependents();

    @Internal
    Collection<? extends ValueCache<?>> getDependents();

    @Internal
    boolean attach(ValueUpdateListener<T> listener);

    @Internal
    boolean detach(ValueUpdateListener<T> listener);

    abstract class Abstract<T> implements ValueCache<T> {
        private final Set<WeakReference<? extends ValueCache<?>>> dependents = new HashSet<>();
        protected final Set<ValueUpdateListener<T>> listeners = new HashSet<>();
        protected final AtomicLong lastUpdate = new AtomicLong(0);
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
            return parent != null && lastUpdate.get() < parent.lastUpdate.get();
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
            cleanupDependents();

            return dependents.stream()
                    .map(WeakReference::get)
                    .collect(Collectors.toSet());
        }

        @Override
        public final boolean outdateDependents() {
            int c = 0;
            final Collection<? extends ValueCache<?>> dependent = getDependents();
            for (ValueCache<?> valueCache : dependent)
                if (valueCache != null && valueCache.outdateCache()) c++;
            return c == dependent.size();
        }

        @Override
        public final void cleanupDependents() {
            dependents.removeIf(ref -> ref.get() == null);
        }

        @Override
        public final boolean attach(ValueUpdateListener<T> listener) {
            return listeners.add(listener);
        }

        @Override
        public final boolean detach(ValueUpdateListener<T> listener) {
            return listeners.remove(listener);
        }

        public final boolean outdateCache() {
            if (isOutdated())
                return false;

            lastUpdate.set(0);
            outdateDependents();
            return true; // todo inspect
        }
    }
}
