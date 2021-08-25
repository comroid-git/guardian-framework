package org.comroid.mutatio.model;

import org.comroid.api.Named;

import java.util.Map;

public interface KeyRef<K, V> extends Named, Ref<V>, Map.Entry<K, V> {
    @Override
    default String getName() {
        return String.valueOf(getKey());
    }

    @Override
    default V getValue() throws ClassCastException {
        return get();
    }

    @Override
    default V setValue(V value) {
        V prev = get();

        return set(value) ? prev : null;
    }

    @Override
    @SuppressWarnings("unchecked")
    default K getKey() throws ClassCastException {
        return (K) get(1);
    }

    default boolean setKey(K key) {
        return set(1, key);
    }
}
