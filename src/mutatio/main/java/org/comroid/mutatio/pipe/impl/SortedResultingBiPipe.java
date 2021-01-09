package org.comroid.mutatio.pipe.impl;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.pipe.BiPipe;
import org.comroid.mutatio.pipe.BiStageAdapter;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.ref.ReferenceMap;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
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

        @Nullable
        @Override
        public V doGet() {
            return refs.streamRefs()
                    .sorted((a, b) -> a.accumulate(b, comparator::compare))
                    .skip(accessedIndex)
                    .findFirst()
                    .flatMap(Rewrapper::wrap)
                    .orElseGet(() -> {
                        if (accessedIndex < refs.size())
                            throw new NoSuchElementException("No element at index " + accessedIndex);
                        return null; // empty
                    });
        }

        @Override
        public K getKey() {
            return refs.streamRefs()
                    .sorted((a, b) -> a.accumulate(b, comparator::compare))
                    .skip(accessedIndex)
                    .findFirst()
                    .filter(KeyedReference.class::isInstance)
                    .map(ref -> ((KeyedReference<K, V>) ref).getKey())
                    .orElseThrow(() -> new NoSuchElementException("No key found"));
        }
    }

    @Override
    public @Nullable KeyedReference<K, V> getReference(K key, boolean createIfAbsent) {
        return accessors.values()
                .stream()
                .filter(ref -> ref.getKey().equals(key))
                .findAny()
                .map(ref -> (KeyedReference<K, V>) ref)
                .orElseGet(() -> tryFindReference(key)
                        .orElseThrow(() -> new IllegalArgumentException("No base ref found for key: " + key)));
    }

    private Optional<KeyedReference<K, V>> tryFindReference(K key) {
        return refs.streamRefs()
                .map(ref -> (KeyedReference<K, V>) ref)
                .filter(ref -> ref.getKey().equals(key))
                .findAny();
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
    public Stream<KeyedReference<K, V>> streamRefs() {
        IntStream.range(0, size())
                .filter(x -> !accessors.containsKey(x))
                .forEach(this::getReference);
        return accessors.values().stream().map(Polyfill::uncheckedCast);
    }

    @Override
    public void forEach(BiConsumer<? super K, ? super V> action) {
        streamRefs().forEach(ref -> action.accept(ref.getKey(), ref.getValue()));
    }
}
