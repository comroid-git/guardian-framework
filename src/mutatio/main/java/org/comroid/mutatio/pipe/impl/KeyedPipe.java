package org.comroid.mutatio.pipe.impl;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.pipe.BiPipe;
import org.comroid.mutatio.pipe.BiStageAdapter;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.ref.ReferenceMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

// todo this class is still shit
public class KeyedPipe<InK, InV, K, V> extends BasicPipe<InV, V> implements BiPipe<K, V> {
    private final Map<K, KeyedReference<K, V>> accessors = new ConcurrentHashMap<>();
    private final boolean isSameKeyType;
    private final EntryIndex entryIndex;

    public KeyedPipe(
            Pipe<InV> old,
            BiStageAdapter<InK, InV, K, V> adapter,
            int autoEmptyLimit
    ) {
        super(old, adapter, autoEmptyLimit);

        this.entryIndex = new EntryIndex();

        KeyedReference<InK, InV> test;
        this.isSameKeyType = adapter instanceof BiStageAdapter.Support.Filter;/*
                || (refs instanceof BiPipe && (test = (KeyedReference<InK, InV>) refs
                .getReference(0)).getKey().getClass()
                .equals(adapter.advance(test).getKey().getClass()));*/
    }

    @Override
    public <Rk, Rv> BiPipe<Rk, Rv> addBiStage(BiStageAdapter<K, V, Rk, Rv> stage) {
        return new KeyedPipe<>(this, stage, autoEmptyLimit);
    }

    @Override
    public synchronized @Nullable KeyedReference<K, V> getReference(K key, boolean createIfAbsent) {
        if (!accessors.containsKey(key) && createIfAbsent) {
            if (refs instanceof BiPipe) {
                BiStageAdapter<InK, InV, K, V> biAdapter = (BiStageAdapter<InK, InV, K, V>) this.adapter;
                InK reversed = biAdapter.reverseKey(key);
                KeyedReference<InK, InV> pre = ((BiPipe<InK, InV>) refs)
                        .getReference(reversed, true);
                KeyedReference<K, V> advance = biAdapter.advance(pre);
                accessors.put(advance.getKey(), advance);
            } else if (adapter instanceof BiStageAdapter.Support.BiSource) {
                // assume V == InV
                BiStageAdapter.Support.BiSource<V, K> biSource = (BiStageAdapter.Support.BiSource<V, K>) adapter;
                V sourceValue = biSource.reverseKey(key);
                Reference<V> baseRef = refs.streamRefs()
                        .map(Polyfill::<Reference<V>>uncheckedCast)
                        .filter(ref -> ref.contentEquals(sourceValue))
                        .findAny()
                        .orElseThrow(() -> new IllegalStateException(String
                                .format("Could not find source value of key in source: %s in %s", key.toString(), refs)));
                KeyedReference<V,V> pre = new KeyedReference.Support.Base<>(null, baseRef);
                KeyedReference<K, V> advance = biSource.advance(pre);
                accessors.put(advance.getKey(), advance);
            } else throw new AssertionError("unreachable");
        }
        if (createIfAbsent && !accessors.containsKey(key))
            throw new InternalError("Unable to generate accessor for key " + key);
        return accessors.get(key);
    }

    @Override
    public ReferenceIndex<? extends KeyedReference<K, V>> entryIndex() {
        return entryIndex;
    }

    @Override
    public boolean containsKey(K key) {
        return accessors.containsKey(key);
    }

    @Override
    public boolean containsValue(V value) {
        return accessors.values()
                .stream()
                .flatMap(Rewrapper::stream)
                .anyMatch(value::equals);
    }

    @Override
    public Pipe<? extends KeyedReference<K, V>> pipe(Predicate<K> filter) {
        return entryIndex.pipe();
    }

    @Override
    public KeyedReference<K, V> getReference(int index) {
        return new ArrayList<>(accessors.entrySet()).get(0).getValue(); // todo LOL
    }

    @Override
    public Stream<KeyedReference<K, V>> streamRefs() {
        // generate accessors todo improve
        //noinspection RedundantSuppression -> also false positive lol
        refs.streamRefs()
                .map(ref -> {
                    KeyedReference<InK, InV> preAdvance;
                    //noinspection unchecked -> false positive
                    BiStageAdapter<InK, InV, K, V> biAdapter = (BiStageAdapter<InK, InV, K, V>) this.adapter;

                    if (ref instanceof KeyedReference) {
                        KeyedReference<InK, InV> castRef = (KeyedReference<InK, InV>) ref;
                        K myKey = biAdapter.convertKey(castRef.getKey());
                        preAdvance = castRef;
                    } else if (this.adapter instanceof BiStageAdapter.Support.BiSource) {
                        InK myKey = ref.into(((BiStageAdapter.Support.BiSource<InV, InK>) biAdapter)::convertKey);
                        preAdvance = new KeyedReference.Support.Base<>(myKey, ref);
                    } else throw new UnsupportedOperationException("Unexpected State");

                    return biAdapter.advance(preAdvance);
                })
                .forEach(ref -> accessors.compute(ref.getKey(), (k, old) -> {
                    if (old == null)
                        return ref;
                    old.rebind(ref);
                    return old;
                }));

        return accessors.values().stream();
    }

    private final class EntryIndex implements ReferenceIndex<KeyedReference<K, V>> {
        private final Map<Integer, Reference<KeyedReference<K, V>>> indexAccessors = new ConcurrentHashMap<>();

        @Override
        public List<KeyedReference<K, V>> unwrap() {
            return Collections.unmodifiableList(new ArrayList<>(accessors.values()));
        }

        @Override
        public int size() {
            return KeyedPipe.this.size();
        }

        @Override
        public boolean add(KeyedReference<K, V> item) {
            return false;
        }

        @Override
        public boolean remove(KeyedReference<K, V> item) {
            return false;
        }

        @Override
        public void clear() {
            KeyedPipe.this.clear();
        }

        @Override
        public Stream<? extends Reference<KeyedReference<K, V>>> streamRefs() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Reference<KeyedReference<K, V>> getReference(int index) {
            return indexAccessors.computeIfAbsent(index, k -> Reference
                    .provided(() -> KeyedPipe.this.getReference(index)));
        }
    }
}
