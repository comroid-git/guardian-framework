package org.comroid.mutatio.pipe;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.pipe.impl.KeyedPipe;
import org.comroid.mutatio.pipe.impl.SortedResultingPipe;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;

import java.util.Comparator;
import java.util.stream.Stream;

public class SortedResultingBiPipe<K, V> extends SortedResultingPipe<V> implements BiPipe<K, V> {
    public SortedResultingBiPipe(BiPipe<K, V> base, Comparator<? super V> comparator) {
        super(base, comparator);
    }

    @Override
    public <Rk, Rv> BiPipe<Rk, Rv> addBiStage(BiStageAdapter<K, V, Rk, Rv> stage) {
        return new KeyedPipe<>(this, stage, autoEmptyLimit);
    }

    @Override
    public KeyedReference<K, V> getReference(int index) {
        return (KeyedReference<K, V>) super.getReference(index);
    }

    @Override
    public Stream<KeyedReference<K, V>> streamRefs() {
        return super.streamRefs().map(Polyfill::uncheckedCast);
    }
}
