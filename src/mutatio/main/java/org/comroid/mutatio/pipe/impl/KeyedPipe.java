package org.comroid.mutatio.pipe.impl;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.pipe.BiPipe;
import org.comroid.mutatio.pipe.BiStageAdapter;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.ref.KeyedReference;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

// todo this class is still shit
public class KeyedPipe<InK, InV, K, V> extends BasicPipe<InV, V> implements BiPipe<K, V> {
    private final Map<Integer, KeyedReference<K, V>> accessors = new ConcurrentHashMap<>();
    private final BiStageAdapter<InK, InV, K, V> adapter;

    public KeyedPipe(
            Pipe<InV> old,
            BiStageAdapter<InK, InV, K, V> adapter,
            int autoEmptyLimit
    ) {
        super(old, adapter, autoEmptyLimit);

        this.adapter = adapter;
    }

    @Override
    public <Rk, Rv> BiPipe<Rk, Rv> addBiStage(BiStageAdapter<K, V, Rk, Rv> stage) {
        return new KeyedPipe<>(this, stage, autoEmptyLimit);
    }

    @Override
    public KeyedReference<K, V> getReference(int index) {
        return accessors.computeIfAbsent(index, key -> adapter.advance(prefabRef(refs.getReference(index))));
    }

    @Override
    public Stream<KeyedReference<K, V>> streamRefs() {
        // generate accessors
        refs.generateAccessors(accessors, adapter, BiStageAdapter::advance);
        return accessors.values().stream();
    }
}
