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
import java.util.stream.Collectors;
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
    public KeyedReference<K, V> getReference(int index) {
        return Polyfill.uncheckedCast(super.getReference(index));
    }

    @Override
    public Stream<KeyedReference<K, V>> streamRefs() {
        return super.streamRefs().map(Polyfill::uncheckedCast);
    }

    private final class EntryIndex implements ReferenceIndex<KeyedReference<K, V>> {
        private final Map<Integer, Reference<KeyedReference<K, V>>> indexAccessors = new ConcurrentHashMap<>();

        @Override
        public List<KeyedReference<K, V>> unwrap() {
            return Collections.unmodifiableList(stream().collect(Collectors.toList()));
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
