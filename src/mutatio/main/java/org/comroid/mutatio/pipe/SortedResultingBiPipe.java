package org.comroid.mutatio.pipe;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.pipe.impl.KeyedPipe;
import org.comroid.mutatio.ref.BiProcessor;
import org.comroid.mutatio.ref.KeyedReference;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SortedResultingBiPipe<K, V> extends KeyedPipe<K, V, K, V> implements BiPipe<K, V> {
    private final Comparator<? super V> comparator;

    public SortedResultingBiPipe(Pipe<V> old, Comparator<? super V> comparator) {
        super(old, BiStageAdapter.filterKey(any -> true), -1);

        this.comparator = comparator;
    }

    @Override
    public <Rk, Rv> BiPipe<Rk, Rv> addBiStage(BiStageAdapter<K, V, Rk, Rv> stage) {
        return new KeyedPipe<>(this, stage, autoEmptyLimit);
    }

    @Override
    public KeyedReference<K, V> getReference(int index) {
        class SubRef extends KeyedReference.Support.Base<K, V> {
            private final int accessedIndex;

            @Override
            public boolean isOutdated() {
                return true;
            }

            public SubRef(int accessedIndex) {
                super(false, null, null);

                this.accessedIndex = accessedIndex;
            }

            private @Nullable KeyedReference<K, V> getRef() {
                List<KeyedReference<K, V>> collect = refs.streamRefs()
                        .map(Polyfill::<KeyedReference<K, V>>uncheckedCast)
                        .filter(Rewrapper::isNonNull)
                        .sorted((a, b) -> a.accumulate(b, comparator::compare))
                        .collect(Collectors.toList());
                if (accessedIndex >= collect.size())
                    return null;
                return collect.get(accessedIndex);
            }

            @Nullable
            @Override
            public V doGet() {
                KeyedReference<K, V> ref = getRef();
                if (ref == null)
                    return null;
                return ref.getValue();
            }

            @Override
            public K getKey() {
                return getRef().getKey();
            }
        }

        return new SubRef(index);
    }
}
