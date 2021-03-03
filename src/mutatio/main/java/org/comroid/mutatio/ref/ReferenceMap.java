package org.comroid.mutatio.ref;

import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.adapter.BiStageAdapter;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.model.RefMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.comroid.api.Polyfill.uncheckedCast;

public class ReferenceMap<K, V>
        extends ReferencePipe.ForMap<Object, Object, K, V>
        implements UncheckedCloseable, RefMap<K, V> {
    public ReferenceMap(
    ) {
        this(null);
    }

    public ReferenceMap(
            @Nullable ReferenceMap<K, V> parent
    ) {
        this(uncheckedCast(parent), uncheckedCast(BiStageAdapter.identity()));
    }

    public <InK, InV> ReferenceMap(
            @Nullable ReferenceMap<InK, InV> parent,
            @NotNull BiStageAdapter<InK, InV, K, V> advancer
    ) {
        super(uncheckedCast(parent), uncheckedCast(advancer));
    }

    @Override
    public final boolean containsKey(Object key) {
        return streamKeys().anyMatch(key::equals);
    }

    @Override
    public final boolean containsValue(Object value) {
        return streamRefs().map(KeyedReference::getValue).anyMatch(value::equals);
    }

    @Override
    public final V get(Object key) {
        KeyedReference<K, V> ref = getReference(key);
        if (ref == null)
            return null;
        return ref.get();
    }

    @Nullable
    @Override
    public final V put(K key, V value) {
        KeyedReference<K, V> ref = getReference(key, true);
        V old = ref.get();
        ref.set(value);
        return old;
    }

    @Override
    public final V remove(Object key) {
        KeyedReference<K, V> ref = getReference(key);
        if (ref == null)
            return null;
        V old = ref.get();
        if (!ref.unset())
            throw new UnsupportedOperationException("Could not unset");
        return old;
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return Collections.unmodifiableSet(streamRefs().collect(Collectors.toSet()));
    }

    @Override
    public void close() {
        for (ValueCache<?> each : getDependents())
            if (each instanceof UncheckedCloseable)
                ((UncheckedCloseable) each).close();
    }

    @Override
    public @Nullable KeyedReference<K, V> getReference(Object key) {
        //noinspection unchecked
        return getReference((K) key, false);
    }
}
