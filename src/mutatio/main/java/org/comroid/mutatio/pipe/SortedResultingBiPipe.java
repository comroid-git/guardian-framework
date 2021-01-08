package org.comroid.mutatio.pipe;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.pipe.impl.KeyedPipe;
import org.comroid.mutatio.ref.BiProcessor;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.ref.ReferenceMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SortedResultingBiPipe<K, V> extends KeyedPipe<K, V, K, V> implements BiPipe<K, V>, ReferenceMap<K, V> {
    private final Comparator<? super V> comparator;
    private final Map<Integer, SubRef> accessors = new ConcurrentHashMap<>();

    public SortedResultingBiPipe(BiPipe<K, V> old, Comparator<? super V> comparator) {
        super(old, BiStageAdapter.filterKey(any -> true), -1);

        this.comparator = comparator;
    }

    @Override
    public <Rk, Rv> BiPipe<Rk, Rv> addBiStage(BiStageAdapter<K, V, Rk, Rv> stage) {
        return new KeyedPipe<>(this, stage, autoEmptyLimit);
    }

    @Override
    public KeyedReference<K, V> getReference(int index) {
        return accessors.computeIfAbsent(index, SubRef::new);
    }

    private final class SubRef extends KeyedReference.Support.Base<K, V> {
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
            KeyedReference<K, V> ref = getRef();
            if (ref == null)
                throw new NoSuchElementException("No element present at index " + accessedIndex);
            return ref.getKey();
        }
    }

    @Override
    public @Nullable KeyedReference<K, V> getReference(K key, boolean createIfAbsent) {
        return accessors.values()
                .stream()
                .filter(ref -> ref.getRef() != null)
                .filter(ref -> ref.getKey().equals(key))
                .findAny()
                .orElse(null);
    }

    @Override
    public ReferenceIndex<? extends KeyedReference<K, V>> entryIndex() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(K key) {
        return streamRefs().map(Map.Entry::getKey).anyMatch(key::equals);
    }

    @Override
    public boolean containsValue(V value) {
        return streamRefs().map(Map.Entry::getValue).anyMatch(value::equals);
    }

    @Override
    public Stream<V> stream() {
        return streamRefs().map(Map.Entry::getValue);
    }

    @Override
    public Pipe<? extends KeyedReference<K, V>> pipe(Predicate<K> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        streamRefs().forEach(ref -> action.accept(ref.getKey(), ref.getValue()));
    }
}
