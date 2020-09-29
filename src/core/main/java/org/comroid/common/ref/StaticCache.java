package org.comroid.common.ref;

import org.comroid.api.Polyfill;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class StaticCache {
    private static final Map<Object, StaticCache> staticCache = new ConcurrentHashMap<>();
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    private final Object owner;

    private StaticCache(Object owner) {
        this.owner = owner;
    }

    public static <T> T access(Object accessor, String key, Supplier<T> supplier) {
        return myCache(accessor).computeIfAbsent(key, supplier);
    }

    public static StaticCache myCache(Object accessor) {
        return staticCache.computeIfAbsent(accessor, StaticCache::new);
    }

    public static void deleteMe(Object accessor) {
        staticCache.remove(accessor);
    }

    private <T> T computeIfAbsent(String key, Supplier<T> supplier) {
        final Object value = cache.computeIfAbsent(key, (k) -> supplier.get());

        return Polyfill.uncheckedCast(value);
    }

    @Override
    public String toString() {
        return String.format("StaticCache{owner=%s}", owner);
    }
}
