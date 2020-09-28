package org.comroid.mutatio.pump;

import org.comroid.api.ExecutorBound;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.pipe.StageAdapter;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;

import java.util.Collection;
import java.util.concurrent.Executor;

public interface Pump<T> extends Pipe<T>, ExecutorBound {
    static <T> Pump<T> create() {
        return create(Runnable::run);
    }

    static <T> Pump<T> create(Executor executor) {
        return new BasicPump<>(executor, ReferenceIndex.create());
    }

    static <T> Pump<T> of(Collection<T> collection) {
        final Pump<T> pump = create();
        collection.stream()
                .map(Reference::constant)
                .map(ref -> ref.map(Object.class::cast))
                .forEach(pump);

        return pump;
    }

    @Override
    <R> Pump<R> addStage(StageAdapter<T, R> stage);

    <R> Pump<R> addStage(Executor executor, StageAdapter<T, R> stage);
}
