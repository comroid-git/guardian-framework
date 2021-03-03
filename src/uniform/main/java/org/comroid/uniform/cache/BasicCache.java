package org.comroid.uniform.cache;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Provider;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BasicCache<K, V> extends AbstractCache<K, V> {
    public static final int DEFAULT_LARGE_THRESHOLD = 250;
    private final @Nullable Provider.Now<V> emptyValueProvider;
    private final Map<K, CacheReference<K, V>> cache;
    private final int largeThreshold;

    public BasicCache(ContextualProvider context) {
        this(context, DEFAULT_LARGE_THRESHOLD);
    }

    public BasicCache(ContextualProvider context, int largeThreshold) {
        this(context, largeThreshold, new ConcurrentHashMap<>());
    }

    protected BasicCache(ContextualProvider context, Map<K, CacheReference<K, V>> map) {
        this(context, DEFAULT_LARGE_THRESHOLD, map);
    }

    protected BasicCache(ContextualProvider context, int largeThreshold, Map<K, CacheReference<K, V>> map) {
        this(context, largeThreshold, map, null);
    }

    protected BasicCache(ContextualProvider context, int largeThreshold,
                         Map<K, CacheReference<K, V>> map,
                         @Nullable Provider.Now<V> emptyValueProvider) {
        super(context);

        this.largeThreshold = largeThreshold;
        this.cache = map;
        this.emptyValueProvider = emptyValueProvider;
    }

    @Override
    protected CacheReference<K, V> advanceIntoCacheRef(Reference<V> reference) {
        return null; //todo
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    public <R> Reference<R> accessor(K key, String name, Reference.Advancer<V, ? extends R> advancer) {
        return null; // todo
    }

    @NotNull
    @Override
    public Iterator<CacheReference<K, V>> iterator() {
        return cache.values().iterator();
    }
}
