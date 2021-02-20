package org.comroid.mutatio.ref;

import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.Pipeable;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class ReferenceMap<K, V> extends ValueCache.Abstract<Void> implements Pipeable<V>, Map<K, V>, UncheckedCloseable {
    private final ReferenceMap<Object, Object> base;
    private final KeyedReference.Advancer<Object, Object, K, V> advancer;
    private final Map<K, KeyedReference<K, V>> accessors;
    private final Function<K, Object> kReverser;
    private final Function<V, Object> vReverser;

    public final KeyedReference.Advancer<?, ?, K, V> getAdvancer() {
        return advancer;
    }

    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }

    protected ReferenceMap(ValueCache<?> parent) {
        super(parent);
    }

    private void validateBaseExists() {
        if (base == null)
            throw new AbstractMethodError();
    }

    @Override
    public final int size() throws AbstractMethodError {
        return base == null ? accessors.size() : base.size();
    }

    public final Stream<K> streamKeys() {
        return Stream.concat(
                base.streamKeys().map(advancer::advanceKey),
                accessors.keySet().stream()
        ).distinct();
    }

    public final Stream<KeyedReference<K, V>> streamRefs() {
        return streamKeys().map(this::getReference);
    }

    public final @Nullable KeyedReference<K, V> getReference(Object k) {
        return getReference(k, false);
    }

    @Contract("!null, false -> _; !null, true -> !null")
    public final KeyedReference<K, V> getReference(Object k, boolean createIfAbsent) {
        K key;
        try {
            //noinspection unchecked
            key = (K) k;
        } catch (ClassCastException cce) {
            throw new IllegalArgumentException("Not a valid key: " + k, cce);
            // todo: Return empty instead?
        }
        KeyedReference<K, V> ref = accessors.get(key);
        if (ref != null && createIfAbsent) {
            ref = computeRef(key);
            if (ref == null)
                ref = KeyedReference.createKey(false, key);
            if (accessors.containsKey(key))
                accessors.get(key).rebind(ref);
            else accessors.put(key, ref);
        }
        return ref;
    }

    public final @Nullable KeyedReference<K, V> computeRef(K key) {
        Object revK = kReverser == null ? null : kReverser.apply(key);
        KeyedReference<K, V> ref;
        if (revK != null && base != null && base.containsKey(revK))
            ref = advancer.advance(base.getReference(revK));
        else ref = KeyedReference.createKey(key);
        accessors.put(key, ref);
        return ref;
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
        KeyedReference<K, V> ref = getReference(key, false);
        if (ref == null)
            return null;
        if (!ref.unset())
            throw new UnsupportedOperationException("Could not unset");
    }

    @Override
    public final void putAll(@NotNull Map<? extends K, ? extends V> m) {
    }

    @Override
    public void clear() {

    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return null;
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return null;
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public ReferenceIndex<? extends V> pipe() {
        return null;
    }
}
