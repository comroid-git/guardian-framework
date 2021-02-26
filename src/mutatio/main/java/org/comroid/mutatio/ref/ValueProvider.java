package org.comroid.mutatio.ref;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.SingleValueCache;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.Function;

public abstract class ValueProvider<I, O> extends SingleValueCache.Abstract<O> {
    protected Function<I, O> overriddenSupplier = null;

    @Override
    public final Rewrapper<? extends Reference<?>> getParent() {
        return () -> super.getParent().into(Reference.class);
    }

    protected ValueProvider(@Nullable ValueProvider<?, ?> parent, @Nullable Executor autocomputor) {
        super(parent, autocomputor);
    }

    protected abstract O doGet(I param);

    public synchronized final O get(I param) {
        O fromCache = getFromCache();
        if (isUpToDate() && fromCache != null) {
            //logger.trace("{} is up to date; does not need computing", toString());
            return fromCache;
        }
        //logger.trace("{} is not up to date; recomputing", toString());
        O value = overriddenSupplier != null ? overriddenSupplier.apply(param) : doGet(param);

        return putIntoCache(value);
    }

    @Override
    public final void computeAndStoreValue() {
        try {
            computeAndStoreValue(null);
        } catch (Throwable t) {
            throw new UnsupportedOperationException(String
                    .format("Parameter required for %s#computeAndStoreValue", getClass().getSimpleName()), t);
        }
    }

    public void computeAndStoreValue(I param) {
        O v = get(param);
        if (isOutdated())
            putIntoCache(v);
    }

    public static abstract class NoParam<T> extends ValueProvider<@Nullable("always") Object, T> {
        protected NoParam(@Nullable ValueProvider<?, ?> parent, @Nullable Executor autocomputor) {
            super(parent, autocomputor);
        }

        protected abstract T doGet();

        @Override
        protected final T doGet(@Nullable("always") Object ignored) {
            return doGet();
        }
    }
}
