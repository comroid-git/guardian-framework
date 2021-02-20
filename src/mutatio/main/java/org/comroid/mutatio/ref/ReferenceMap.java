package org.comroid.mutatio.ref;

import org.comroid.abstr.AbstractMap;
import org.comroid.api.Polyfill;
import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.BiStageAdapter;
import org.comroid.mutatio.pipe.Pipeable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ReferenceMap<InK, InV, K, V>
        extends ReferenceAtlas.ForMap<InK, InV, K, V>
        implements AbstractMap<K, V>, Pipeable<V>, UncheckedCloseable {
    public ReferenceMap(
    ) {
        this(null);
    }

    public ReferenceMap(
            @Nullable ReferenceMap<?, ?, K, V> parent
    ) {
        this(Polyfill.uncheckedCast(parent), Polyfill.uncheckedCast(BiStageAdapter.identity()), Polyfill.uncheckedCast(Function.identity()));
    }

    public ReferenceMap(
            @Nullable ReferenceMap<?, ?, InK, InV> parent,
            @NotNull BiStageAdapter<InK, InV, K, V> advancer,
            @NotNull Function<K, InK> keyReverser
    ) {
        super(parent, advancer, keyReverser);
    }

    public final @Nullable KeyedReference<K, V> getReference(Object k) {
        //noinspection unchecked
        return getReference((K) k, false);
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
}
