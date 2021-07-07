package org.comroid.mutatio.model;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public interface RefPipe<InK, InV, K, V> extends RefAtlas<InK, K, InV, V>, BiConsumer<InK, InV> {
    @Nullable Executor getStageExecutor();

    @Override
    default void accept(InK inK, InV inV) {
        callDependentStages(getStageExecutor() == null ? Runnable::run : getStageExecutor(), inK, inV);
    }

    void callDependentStages(Executor executor, InK inK, InV inV);
}
