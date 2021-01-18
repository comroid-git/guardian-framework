package org.comroid.uniform.cache;

import org.comroid.api.ContextualProvider;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.ref.ReferenceMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class AbstractCache<K, V> implements Cache<K, V> {
    private final ReferenceMap<K, V> cache;
    private final ContextualProvider context;

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    protected AbstractCache(ContextualProvider context) {
        this(context, ReferenceMap.create(new ConcurrentHashMap<>()));
    }

    protected AbstractCache(ContextualProvider context, ReferenceMap<K, V> cache) {
        this.context = context;
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
    public Stream<? extends KeyedReference<K, V>> streamRefs() {
        return cache.streamRefs();
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
    public ReferenceIndex<? extends KeyedReference<K, V>> entryIndex() {
        return cache.entryIndex();
    }
}
