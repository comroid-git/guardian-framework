package org.comroid.mutatio.ref;

import org.comroid.abstr.AbstractMap;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.Pipeable;
import org.jetbrains.annotations.Nullable;

public interface RefMap<K, V> extends ValueCache<Void>, AbstractMap<K, V>, Pipeable<V> {
    @Nullable KeyedReference<K, V> getReference(Object k);

    @Override
    boolean containsKey(Object key);

    @Override
    boolean containsValue(Object value);
}
