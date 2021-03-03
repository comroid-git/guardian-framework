package org.comroid.mutatio.model;

import org.comroid.api.MutableState;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.ref.Reference;

public interface RefAtlas<InK, K, In, V, InRef extends Reference<In>, OutRef extends Reference<V>>
        extends RefOPs<K, V, OutRef>, ValueCache<Void>, MutableState, RefContainer<K, V, OutRef> {
    ReferenceStageAdapter<InK, K, In, V, InRef, OutRef> getAdvancer();

    @Override
    boolean isMutable();

    @Override
    boolean setMutable(boolean state);

    void clear();

    InRef getInputReference(InK key, boolean createIfAbsent);
}
