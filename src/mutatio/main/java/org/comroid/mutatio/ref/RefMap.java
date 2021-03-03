package org.comroid.mutatio.ref;

import org.comroid.abstr.AbstractMap;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.Pipeable;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

@Experimental
public interface RefMap<K, V> extends RefPipe<Object, Object, K, V>, ValueCache<Void>, AbstractMap<K, V>, Pipeable<V> {
    @Override
    default int size() {
        return entrySet().size();
    }
    
    @Nullable
    KeyedReference<K, V> getReference(Object key);
}
