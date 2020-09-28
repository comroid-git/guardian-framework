package org.comroid.uniform.cache;

import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.ref.ReferenceMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class AbstractCache<K, V> implements Cache<K, V> {
    private final ReferenceMap<K, V> cache;

    protected AbstractCache() {
        this(ReferenceMap.create(new ConcurrentHashMap<>()));
    }

    protected AbstractCache(ReferenceMap<K, V> cache) {
        this.cache = cache;
    }

    protected abstract CacheReference<K, V> advanceIntoCacheRef(Reference<V> reference);

    @Override
    public boolean containsKey(K key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) {
        return stream().anyMatch(value::equals);
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public final Stream<? extends KeyedReference<K, V>> stream(Predicate<K> filter) {
        return cache.stream(filter);
    }

    @Override
    public Pipe<? extends KeyedReference<K, V>> pipe(Predicate<K> filter) {
        return cache.pipe(filter);
    }

    @Override
    public @NotNull KeyedReference<K, V> getReference(K key, boolean createIfAbsent) { // todo lol why is this suggestion here
        return Objects.requireNonNull(cache.getReference(key, createIfAbsent), "please contact the developer");
    }

    @Override
    public ReferenceIndex<? extends Map.Entry<K, V>> entryIndex() {
        return cache.entryIndex();
    }
}
