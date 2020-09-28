package org.comroid.mutatio.ref;

import org.comroid.mutatio.pipe.BiPipe;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.pump.Pump;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

public interface ReferenceMap<K, V> extends Pipeable<V> {
    static <K, V> ReferenceMap<K, V> create() {
        return create(new ConcurrentHashMap<>());
    }

    static <K, V> ReferenceMap<K, V> create(Map<K, KeyedReference<K, V>> refMap) {
        return new ReferenceMap.Support.Basic<K, V>(refMap);
    }

    default boolean put(K key, V value) {
        return getReference(key, true).set(value);
    }

    default KeyedReference<K, V> getReference(K key) {
        return getReference(key, false);
    }

    /**
     * Gets a reference to the value at the specified key in the map.
     * As described by the {@linkplain Contract method contract}; this method will
     * <ul>
     *     <li>Fail, if the first parameter is {@code null}</li>
     *     <li>Return a {@link Nullable} Reference, if the second parameter is {@code false}</li>
     *     <li>Return q {@link NotNull} Reference, if the second parameter is {@code true}</li>
     * </ul>
     *
     * @param key            The key to look at.
     * @param createIfAbsent Whether to create the reference if its non-existent
     * @return A {@link Reference}, or {@code null}
     */
    @Contract("null, _ -> fail; !null, false -> _; !null, true -> !null")
    @Nullable KeyedReference<K, V> getReference(K key, boolean createIfAbsent);

    ReferenceIndex<? extends Map.Entry<K, V>> entryIndex();

    default V get(K key) {
        return getReference(key, true).get();
    }

    default Optional<V> wrap(K key) {
        return getReference(key, true).wrap();
    }

    default Processor<V> process(K key) {
        return getReference(key, true).process();
    }

    default @NotNull V requireNonNull(K key) {
        return getReference(key, true).requireNonNull();
    }

    default @NotNull V requireNonNull(K key, String message) {
        return getReference(key, true).requireNonNull(message);
    }

    int size();

    boolean containsKey(K key);

    boolean containsValue(V value);

    default Stream<? extends KeyedReference<K, V>> streamRefs() {
        return stream(any -> true);
    }

    default Stream<? extends V> stream() {
        return stream(any -> true).map(Reference::get);
    }

    Stream<? extends KeyedReference<K, V>> stream(Predicate<K> filter);

    @Override
    default Pipe<? extends V> pipe() {
        return entryIndex()
                .pipe()
                .map(Map.Entry::getValue);
    }

    Pipe<? extends KeyedReference<K, V>> pipe(Predicate<K> filter);

    @Override
    default Pump<? extends V> pump(Executor executor) {
        return pipe().pump(executor);
    }

    default BiPipe<?, ?, ? extends K, ? extends V> biPipe() {
        return entryIndex()
                .pipe()
                .bi(Map.Entry::getValue)
                .mapFirst(Map.Entry::getKey);
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    default boolean set(K key, V newValue) {
        return getReference(key).set(newValue);
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    default @Nullable V compute(K key, Function<V, V> computor) {
        return getReference(key, true).compute(computor);
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    default @Nullable V computeIfPresent(K key, Function<V, V> computor) {
        return getReference(key, true).computeIfPresent(computor);
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    default @Nullable V computeIfAbsent(K key, Supplier<V> supplier) {
        return getReference(key, true).computeIfAbsent(supplier);
    }

    void forEach(BiConsumer<K, V> action);

    void clear();

    final class Support {
        public static class Basic<K, V> implements ReferenceMap<K, V> {
            private final EntryIndex entryIndex = new EntryIndex();
            private final Map<K, KeyedReference<K, V>> refMap;

            public Basic(Map<K, KeyedReference<K, V>> refMap) {
                this.refMap = refMap;
            }

            @Override
            public @Nullable KeyedReference<K, V> getReference(K key, boolean createIfAbsent) {
                if (!containsKey(key) && createIfAbsent)
                    return refMap.computeIfAbsent(key, KeyedReference::create);
                return refMap.get(key);
            }

            @Override
            public ReferenceIndex<? extends Map.Entry<K, V>> entryIndex() {
                return entryIndex;
            }

            @Override
            public int size() {
                return refMap.size();
            }

            @Override
            public boolean containsKey(K key) {
                return refMap.containsKey(key);
            }

            @Override
            public boolean containsValue(V value) {
                return refMap.values()
                        .stream()
                        .anyMatch(ref -> ref.test(value::equals));
            }

            @Override
            public Stream<KeyedReference<K, V>> stream(Predicate<K> filter) {
                return entryIndex.stream();
            }

            @Override
            public Pipe<KeyedReference<K, V>> pipe(Predicate<K> filter) {
                return entryIndex.pipe();
            }

            @Override
            public void forEach(BiConsumer<K, V> action) {
                refMap.forEach((k, ref) -> ref.consume(it -> action.accept(k, it)));
            }

            @Override
            public void clear() {
                refMap.clear();
            }

            private final class EntryIndex implements ReferenceIndex<KeyedReference<K, V>> {
                private final Map<Integer, Reference<KeyedReference<K, V>>> indexAccessors = new ConcurrentHashMap<>();

                @Override
                public List<KeyedReference<K, V>> unwrap() {
                    return new ArrayList<>(refMap.values());
                }

                @Override
                public int size() {
                    return Basic.this.size();
                }

                @Override
                public boolean add(KeyedReference<K, V> ref) {
                    final K key = ref.getKey();

                    if (!containsKey(key)) {
                        refMap.put(key, ref);
                        return true;
                    }

                    refMap.compute(key, (k, v) -> {
                        if (v == null)
                            return ref;
                        v.rebind(ref);
                        return v;
                    });
                    return true;
                }

                @Override
                public boolean remove(KeyedReference<K, V> ref) {
                    final K key = ref.getKey();

                    return refMap.remove(key) != null;
                }

                @Override
                public void clear() {
                    Basic.this.clear();
                }

                @Override
                public Reference<KeyedReference<K, V>> getReference(int index) {
                    return indexAccessors.computeIfAbsent(index, k -> Reference.provided(() -> unwrap().get(index)));
                }
            }
        }
    }
}
