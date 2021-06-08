package org.comroid.mutatio.ref;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Rewrapper;
import org.comroid.api.ValueProvider;
import org.comroid.mutatio.cache.SingleValueCache;
import org.comroid.mutatio.cache.ValueCache;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.concurrent.Executor;
import java.util.function.Function;

public abstract class CachedValueProvider<I, O> extends SingleValueCache.Abstract<O> implements ValueProvider<I, O> {
    private static final Logger logger = LogManager.getLogger();
    protected Function<I, O> overriddenSupplier = null;
    private @Nullable String name;
    private WeakReference<I> lastParam;

    @Override
    public final Rewrapper<? extends Reference<?>> getParent() {
        return () -> super.getParent().into(Reference.class);
    }

    @Override
    public String getName() {
        String yield = name;
        if (yield == null) {
            Class<? extends CachedValueProvider> cls = getClass();
            String simpleName = cls.getSimpleName();
            yield = (simpleName.isEmpty() ? cls.getName() : simpleName);
        }
        return "ValueCache@" + hashCode() + ":'" + yield + "'";
    }

    protected CachedValueProvider(@Nullable ValueCache<?> parent, @Nullable Executor autocomputor) {
        super(parent, autocomputor);
    }

    @Override
    public boolean setName(@Nullable String name) {
        this.name = name;
        return true;
    }

    protected abstract O doGet(I param);

    @Override
    public synchronized final O get(I param) {
        if (param != null && lastParam != null) { // todo Inspect
            I lastParam = this.lastParam.get();
            if (lastParam != null && lastParam.equals(param)) {
                logger.log(Level.ALL, "{} is accessed again with same parameter; returning cached value", getName());
                return getFromCache();
            }
            logger.log(Level.ALL, "{} is accessed with different parameter; outdating cache", getName());
            outdateCache();
        }

        O fromCache = getFromCache();
        if (isUpToDate() && fromCache != null) {
            logger.log(Level.ALL, "{} is up to date; does not need computing", getName());
            return fromCache;
        }
        logger.log(Level.ALL, "{} is not up to date; recomputing", getName());
        O value = overriddenSupplier != null ? overriddenSupplier.apply(param) : doGet(param);

        this.lastParam = new WeakReference<>(param);
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

    public static abstract class NoParam<T> extends CachedValueProvider<@Nullable("always") Object, T> {
        protected NoParam(@Nullable SingleValueCache<?> parent, @Nullable Executor autocomputor) {
            super(parent, autocomputor);
        }

        protected abstract T doGet();

        @Override
        protected final T doGet(@Nullable("always") Object ignored) {
            return doGet();
        }
    }
}
