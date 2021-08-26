package org.comroid.mutatio.model;

import org.comroid.mutatio.ref.KeyedReference;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public interface RefContainer<K, V> extends RefOPs<K, V, KeyedReference<K, V>> {
    Stream<K> streamKeys();

    Stream<KeyedReference<K, V>> streamRefs();

    Stream<V> streamValues();

    int size();

    boolean removeRef(K key);

    default @NotNull KeyedReference<K, V> getReference(K key) {
        KeyedReference<K, V> ref = getReference(key, false);
        if (ref == null)
            return KeyedReference.emptyValue(key);
        return ref;
    }

    @Contract("!null, false -> _; !null, true -> !null; null, _ -> fail")
    KeyedReference<K, V> getReference(K key, boolean createIfAbsent);

    void forEach(final Consumer<? super V> action);

    void forEach(final BiConsumer<? super K, ? super V> action);

    default RefContainer<K, V> immutable() {
        return map(Function.identity()); // adds map stage without backwards converter => immutable stage
    }

    @ApiStatus.Experimental
    default Collection<V> unwrap() {
        final ArrayList<V> list = new ArrayList<>();
        streamValues().forEach(list::add);
        return list;
    }
}
