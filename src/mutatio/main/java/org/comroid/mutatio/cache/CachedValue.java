package org.comroid.mutatio.cache;

import org.comroid.api.Rewrapper;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public interface CachedValue<T> {
    Rewrapper<? extends CachedValue<?>> getParent();

    /**
     * @return Whether {@link #outdate()} was called on this container and it hasn't been {@linkplain #update(Object) updated} yet.
     */
    boolean isOutdated();

    default boolean isUpToDate() {
        return !isOutdated();
    }

    @Internal
    Collection<? extends CachedValue<?>> getDependents();

    void cleanupDependents();

    /**
     * <p>Implementation Note: The value should already be stored when this method is called.</p>
     *
     * @return The new Value
     */
    T update(T withValue);

    /**
     * @return Whether the reference became outdated with this call.
     */
    boolean outdate();

    default ValueUpdateListener<T> onChange(Consumer<T> consumer) {
        return ValueUpdateListener.ofConsumer(this, consumer);
    }

    @Internal
    default boolean outdateChildren() {
        int c = 0;

        final Collection<? extends CachedValue<?>> dependent = getDependents();
        for (CachedValue<?> cachedValue : dependent)
            if (cachedValue.outdate()) c++;
        return c == dependent.size();
    }

    @Internal
    boolean addDependent(CachedValue<?> dependency);

    @Internal
    boolean attach(ValueUpdateListener<T> listener);

    @Internal
    boolean detach(ValueUpdateListener<T> listener);

    abstract class Abstract<T> implements CachedValue<T> {
        protected final Set<WeakReference<CachedValue<?>>> dependents = new HashSet<>();
        private final CachedValue<?> parent;
        private final Set<ValueUpdateListener<T>> listeners = new HashSet<>();
        private final AtomicBoolean outdated = new AtomicBoolean(true);

        @Override
        public Rewrapper<? extends CachedValue<?>> getParent() {
            return () -> parent;
        }

        @Override
        public boolean isOutdated() {
            return outdated.get() || (parent != null && parent.isOutdated());
        }

        @Override
        public final Collection<? extends CachedValue<?>> getDependents() {
            cleanupDependents();

            return dependents.stream()
                    .map(WeakReference::get)
                    .collect(Collectors.toSet());
        }

        protected Abstract(@Nullable CachedValue<?> parent) {
            this.parent = parent;

            if (parent != null && !parent.addDependent(this))
                throw new RuntimeException("Could not add new dependency to parent " + parent);
        }

        @Override
        public final void cleanupDependents() {
            dependents.removeIf(ref -> ref.get() == null);
        }

        @Override
        public T update(T withValue) {
            outdated.set(false);
            outdateChildren();
            listeners.forEach(listener -> listener.acceptNewValue(withValue));
            return withValue;
        }

        @Override
        public boolean outdate() {
            if (isOutdated())
                return false;

            outdated.set(true);
            outdateChildren();
            return true; // todo inspect
        }

        @Override
        public boolean addDependent(CachedValue<?> dependency) {
            return dependents.add(new WeakReference<>(dependency));
        }

        @Override
        public boolean attach(ValueUpdateListener<T> listener) {
            return listeners.add(listener);
        }

        @Override
        public boolean detach(ValueUpdateListener<T> listener) {
            return listeners.remove(listener);
        }

        @Override
        public String toString() {
            return String.format("AbstractCachedValue{outdated=%s}", outdated);
        }
    }
}
