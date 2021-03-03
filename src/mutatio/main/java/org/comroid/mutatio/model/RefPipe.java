package org.comroid.mutatio.model;

import org.comroid.mutatio.ref.KeyedReference;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface RefPipe<InK, InV, K, V> extends RefAtlas<InK, K, InV, V, KeyedReference<InK, InV>, KeyedReference<K, V>>, BiConsumer<InK, InV> {
    @Nullable Executor getStageExecutor();

    @Override
    void accept(InK inK, InV inV);

    void callDependentStages(Executor executor, InK inK, InV inV);
}
