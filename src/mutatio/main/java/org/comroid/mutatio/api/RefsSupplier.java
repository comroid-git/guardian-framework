package org.comroid.mutatio.api;

import org.comroid.annotations.Upgrade;
import org.comroid.api.StreamSupplier;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.ref.ReferenceList;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface RefsSupplier<K, V> extends StreamSupplier<V> {
    @Upgrade
    static <K, V> RefsSupplier<K, V> upgrade(StreamSupplier<V> old) {
        //noinspection unchecked
        return () -> (RefContainer<K, V>) old.stream().collect(Collectors.toCollection(ReferenceList::new));
    }

    @Override
    default Stream<V> stream() {
        return refs().streamValues();
    }

    RefContainer<K, V> refs();

    interface This<K, V> extends RefsSupplier<K, V>, RefContainer<K, V> {
        @Override
        default RefContainer<K, V> refs() {
            return this;
        }
    }
}
