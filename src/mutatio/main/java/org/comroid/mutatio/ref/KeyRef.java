package org.comroid.mutatio.ref;

import org.comroid.mutatio.model.Ref;

import java.util.Map;

public interface KeyRef<K, V> extends Ref<V>, Map.Entry<K, V> {
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
