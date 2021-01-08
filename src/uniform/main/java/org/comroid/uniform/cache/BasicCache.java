package org.comroid.uniform.cache;

import org.comroid.api.Provider;
import org.comroid.mutatio.ref.Processor;
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

    public BasicCache() {
        this(DEFAULT_LARGE_THRESHOLD);
    }

    public BasicCache(int largeThreshold) {
        this(largeThreshold, new ConcurrentHashMap<>());
    }

    protected BasicCache(Map<K, CacheReference<K, V>> map) {
        this(DEFAULT_LARGE_THRESHOLD, map);
    }

    protected BasicCache(int largeThreshold, Map<K, CacheReference<K, V>> map) {
        this(largeThreshold, map, null);
    }

    protected BasicCache(int largeThreshold,
                         Map<K, CacheReference<K, V>> map,
                         @Nullable Provider.Now<V> emptyValueProvider) {
        super();

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
    public <R> Processor<R> accessor(K key, String name, Processor.Advancer<V, ? extends R> advancer) {
        return null;
    }

    @NotNull
    @Override
    public Iterator<CacheReference<K, V>> iterator() {
        return cache.values().iterator();
    }

    @Override
    public void clear() {
        cache.clear();
    }
}
