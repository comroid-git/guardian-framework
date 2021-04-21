package org.comroid.mutatio.api;

import org.comroid.api.StreamSupplier;
import org.comroid.mutatio.model.RefContainer;

import java.util.stream.Stream;

public interface RefsSupplier<K, V> extends StreamSupplier<V> {
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
