package org.comroid.mutatio.ref;

import org.comroid.api.MutableState;
import org.comroid.mutatio.cache.ValueCache;
import org.jetbrains.annotations.Contract;

import java.util.stream.Stream;

public interface RefAtlas<InK, K, In, V, InRef extends Reference<In>, OutRef extends Reference<V>>
        extends ValueCache<Void>, MutableState {
    @Override
    boolean isMutable();

    @Override
    boolean setMutable(boolean state);

    int size();

    boolean removeRef(K key);

    void clear();

    Stream<K> streamKeys();

    Stream<OutRef> streamRefs();

    Stream<V> streamValues();

    InRef getInputReference(InK key, boolean createIfAbsent);

    @Contract("!null, false -> _; !null, true -> !null; null, _ -> fail")
    OutRef getReference(K key, boolean createIfAbsent);
}
