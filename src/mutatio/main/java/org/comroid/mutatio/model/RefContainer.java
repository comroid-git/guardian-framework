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

    @Contract("!null, false -> _; !null, true -> !null; null, _ -> fail")
    KeyedReference<K, V> getReference(K key, boolean createIfAbsent);
}
