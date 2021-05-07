package org.comroid.mutatio.model;

import org.comroid.abstr.AbstractMap;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.ref.KeyedReference;
import org.jetbrains.annotations.ApiStatus.Experimental;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Experimental
public interface RefMap<K, V> extends RefAtlas<Object, K, Object, V>, ValueCache<Void>, AbstractMap<K, V>, Pipeable<V> {
    @Override
    default int size() {
        return entrySet().size();
    }

    KeyedReference<K, V> getReference(Object key);

    void forEach(final Consumer<? super V> action);

    void forEach(final BiConsumer<? super K, ? super V> action);

    default boolean hasValue(K key) {
        return containsKey(key) && getReference(key, false).isNonNull();
    }
}
