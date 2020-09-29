package org.comroid.common.map;

import org.comroid.mutatio.ref.OutdateableReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * @deprecated WIP
 */
@Deprecated
public class RecomputedMap<K, V, R> implements Map<K, R> {
    private final Map<K, V> base;
    private final Map<K, OutdateableReference<R>> recomputed;
    private final BiFunction<K, @Nullable V, R> recomputor;

    public final Map<K, V> getBase() {
        return base;
    }

    @Override
    public final boolean isEmpty() {
        return base.isEmpty();
    }

    public RecomputedMap(Map<K, V> base, BiFunction<K, @Nullable V, R> recomputor) {
        this(base, new ConcurrentHashMap<>(), recomputor);
    }

    public RecomputedMap(Map<K, V> base, Map<K, OutdateableReference<R>> recomputed, BiFunction<K, V, R> recomputor) {
        this.base = base;
        this.recomputed = recomputed;
        this.recomputor = recomputor;
    }

    protected R recompute(K key, @Nullable V value) {
        return recomputor.apply(key, value);
    }

    @Override
    public final int size() {
        return base.size();
    }

    @Override
    public final boolean containsKey(Object key) {
        return base.containsKey(key);
    }

    @Override
    public final boolean containsValue(Object value) {
        return base.containsValue(value);
    }

    @Override
    public final R get(Object k) {
        //noinspection unchecked
        final K key = (K) k;
        final OutdateableReference<R> ref = compRef(key);

        if (ref.isOutdated()) {
            final R value = recompute(key, base.getOrDefault(key, null));

            if (value != null)
                return ref.update(value);
        }

        return ref.get();
    }

    private OutdateableReference<R> compRef(K key) {
        return recomputed.computeIfAbsent(key, k -> new OutdateableReference<>());
    }

    @Nullable
    @Override
    public R put(K key, R value) {
        return null;
    }

    @Override
    public R remove(Object key) {
        return null;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends R> m) {

    }

    @Override
    public void clear() {

    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return null;
    }

    @NotNull
    @Override
    public Collection<R> values() {
        return null;
    }

    @NotNull
    @Override
    public Set<Entry<K, R>> entrySet() {
        return null;
    }
}
