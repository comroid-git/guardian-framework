package org.comroid.uniform.cache;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
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
    private final int largeThreshold;

    @Override
    protected CacheReference<K, V> createEmptyRef(K key) {
        return new CacheReference<>(key);
    }

    public BasicCache(ContextualProvider context) {
        this(context, DEFAULT_LARGE_THRESHOLD);
    }

    public BasicCache(ContextualProvider context, int largeThreshold) {
        this(context, largeThreshold, null);
    }

    protected BasicCache(ContextualProvider context, int largeThreshold,
                         @Nullable Provider.Now<V> emptyValueProvider) {
        super(context);

        this.largeThreshold = largeThreshold;
        this.emptyValueProvider = emptyValueProvider;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @NotNull
    @Override
    public Iterator<CacheReference<K, V>> iterator() {
        return streamRefs().map(Polyfill::<CacheReference<K, V>>uncheckedCast).iterator();
    }
}
