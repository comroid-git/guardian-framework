package org.comroid.mutatio.ref;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;

@Deprecated
public final class BiProcessor<K, V> extends KeyedReference<K, V> {
    public BiProcessor(K key, Reference<V> valueHolder) {
        super(key, valueHolder);
    }

    protected BiProcessor(K key, boolean mutable) {
        super(key, mutable);
    }

    protected BiProcessor(K key, boolean mutable, Executor autoComputor) {
        super(key, mutable, autoComputor);
    }

    protected BiProcessor(K key, @Nullable V initialValue, boolean mutable) {
        super(key, initialValue, mutable);
    }

    protected BiProcessor(K key, @Nullable V initialValue, boolean mutable, Executor autoComputor) {
        super(key, initialValue, mutable, autoComputor);
    }
}
