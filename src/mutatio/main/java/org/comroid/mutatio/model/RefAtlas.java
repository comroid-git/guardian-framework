package org.comroid.mutatio.model;

import org.comroid.api.MutableState;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.api.RefsSupplier;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.ReferencePipe;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public interface RefAtlas<InK, K, In, V>
        extends ValueCache<Void>, MutableState, RefsSupplier.This<K, V> {
    ReferenceStageAdapter<InK, K, In, V, KeyedReference<InK, In>, KeyedReference<K, V>> getAdvancer();

    @Override
    boolean isMutable();

    @Override
    default <X, Y> ReferencePipe<K, V, X, Y> addStage(
            ReferenceStageAdapter<K, X, V, Y, KeyedReference<K, V>, KeyedReference<X, Y>> adapter,
            @Nullable Comparator<KeyedReference<X, Y>> comparator,
            @Nullable Executor executor
    ) {
        if (executor == null && this instanceof RefPipe)
            executor = ((RefPipe<?, ?, ?, ?>) this).getStageExecutor();
        return new ReferencePipe<>(
                Polyfill.uncheckedCast(this),
                Polyfill.uncheckedCast(adapter),
                comparator,
                executor
        );
    }

    @Override
    boolean setMutable(boolean state);

    void clear();

    Stream<KeyedReference<InK, In>> streamInputRefs();

    KeyedReference<InK, In> getInputReference(InK key, boolean createIfAbsent);
}
