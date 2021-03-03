package org.comroid.mutatio.model;

import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.Contract;

import java.util.stream.Stream;

public interface RefContainer<K, V, OutRef extends Reference<V>> {
    Stream<K> streamKeys();

    Stream<OutRef> streamRefs();

    Stream<V> streamValues();

    int size();

    boolean removeRef(K key);

    @Contract("!null, false -> _; !null, true -> !null; null, _ -> fail")
    OutRef getReference(K key, boolean createIfAbsent);
}
