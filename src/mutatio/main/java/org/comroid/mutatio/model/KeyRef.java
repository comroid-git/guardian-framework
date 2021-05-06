package org.comroid.mutatio.model;

import org.comroid.api.Named;

import java.util.Map;

public interface KeyRef<K, V> extends Named, Ref<V>, Map.Entry<K, V> {
    @Override
    default String getName() {
        return String.valueOf(getKey());
    }

    @Override
    default V getValue() {
        return get();
    }

    @Override
    default V setValue(V value) {
        V prev = get();

        return set(value) ? prev : null;
    }
}
