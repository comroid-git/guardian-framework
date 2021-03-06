package org.comroid.mutatio.model;

import org.comroid.mutatio.ref.KeyedReference;
import org.jetbrains.annotations.Contract;

import java.util.stream.Stream;

public interface RefContainer<K, V> extends RefOPs<K, V, KeyedReference<K, V>> {
    Stream<K> streamKeys();

    Stream<KeyedReference<K, V>> streamRefs();

    Stream<V> streamValues();

    int size();

    boolean removeRef(K key);

    default KeyedReference<K, V> getReference(K key) {
        KeyedReference<K, V> ref = getReference(key, false);
        if (ref == null)
            return KeyedReference.emptyValue(key);
        return ref;
    }

    @Contract("!null, false -> _; !null, true -> !null; null, _ -> fail")
    KeyedReference<K, V> getReference(K key, boolean createIfAbsent);
}
