package org.comroid.mutatio.pipe.impl;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.pipe.BiPipe;
import org.comroid.mutatio.pipe.BiStageAdapter;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class BasicBiPipe<InK, InV, K, V> extends BasicPipe<InV, V> implements BiPipe<K, V> {
    private final Map<K, KeyedReference<K, V>> accessors = new ConcurrentHashMap<>();
    private final boolean isSameKeyType;
    private final EntryIndex entryIndex;

    public BasicBiPipe(
            BiPipe<InK, InV> old,
            BiStageAdapter<InK, InV, K, V> adapter,
            int autoEmptyLimit
    ) {
        super(old, adapter, autoEmptyLimit);

        this.entryIndex = new EntryIndex();

        KeyedReference<InK, InV> test;
        this.isSameKeyType = adapter instanceof BiStageAdapter.Support.Filter
                || (test = (KeyedReference<InK, InV>) refs.getReference(0)).getKey().getClass()
                .equals(adapter.advance(test).getKey().getClass());
        refreshAccessors();
    }

    @SuppressWarnings({"SuspiciousMethodCalls", "unchecked"})
    private void refreshAccessors() {
        Stream<? extends KeyedReference<InK, InV>> stream = ((BiPipe<InK, InV>) refs).streamRefs();
        if (isSameKeyType)
            stream = stream.filter(ref -> !accessors.containsKey(ref.getKey()));
        stream.forEach(ref -> {
            final KeyedReference<K, V> advance = ((BiStageAdapter<InK, InV, K, V>) adapter).advance(ref);
            if (advance != null)
                accessors.put(advance.getKey(), advance);
        });
    }

    @Override
    public <Rk, Rv> BiPipe<Rk, Rv> addBiStage(BiStageAdapter<K, V, Rk, Rv> stage) {
        return new BasicBiPipe<>(this, stage, autoEmptyLimit);
    }

    @Override
    public @Nullable KeyedReference<K, V> getReference(K key, boolean createIfAbsent) {
        refreshAccessors();
        return accessors.get(key);
    }

    @Override
    public ReferenceIndex<? extends Map.Entry<K, V>> entryIndex() {
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
    public Stream<? extends KeyedReference<K, V>> stream(Predicate<K> filter) {
        return accessors.entrySet()
                .stream()
                .filter(entry -> filter.test(entry.getKey()))
                .map(Map.Entry::getValue);
    }

    @Override
    public Pipe<? extends KeyedReference<K, V>> pipe(Predicate<K> filter) {
        return entryIndex.pipe();
    }

    private final class EntryIndex implements ReferenceIndex<KeyedReference<K, V>> {
        private final Map<Integer, Reference<KeyedReference<K, V>>> indexAccessors = new ConcurrentHashMap<>();

        @Override
        public List<KeyedReference<K, V>> unwrap() {
            refreshAccessors();
            return Collections.unmodifiableList(new ArrayList<>(accessors.values()));
        }

        @Override
        public int size() {
            return BasicBiPipe.this.size();
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
            BasicBiPipe.this.clear();
        }

        @Override
        public Reference<KeyedReference<K, V>> getReference(int index) {
            return indexAccessors.computeIfAbsent(index, k -> Reference
                    .provided(() -> (KeyedReference<K, V>) BasicBiPipe.this.getReference(index)));
        }
    }
}
