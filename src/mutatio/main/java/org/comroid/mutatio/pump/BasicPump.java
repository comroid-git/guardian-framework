package org.comroid.mutatio.pump;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.pipe.BasicPipe;
import org.comroid.mutatio.pipe.StageAdapter;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executor;

public class BasicPump<O, T> extends BasicPipe<O, T> implements Pump<T> {
    private final Collection<Pump<?>> subStages = new ArrayList<>();
    private final Executor executor;

    @Override
    public Executor getExecutor() {
        return executor;
    }

    public BasicPump(ReferenceIndex<O> old) {
        this(Runnable::run, old);
    }

    public BasicPump(Executor executor, ReferenceIndex<O> old) {
        //noinspection unchecked
        this(executor, old, StageAdapter.map(it -> (T) it));
    }

    public BasicPump(Executor executor, ReferenceIndex<O> old, StageAdapter<O, T> adapter) {
        super(old, adapter, 50);

        this.executor = executor;
    }

    @Override
    public <R> Pump<R> addStage(StageAdapter<T, R> stage) {
        return addStage(executor, stage);
    }

    @Override
    public <R> Pump<R> addStage(Executor executor, StageAdapter<T, R> stage) {
        return new BasicPump<>(executor, this, stage);
    }

    @Override
    public void accept(Reference<Object> in) {
        //noinspection unchecked
        final O item = (O) in.get();
        refs.add(item);

        final Reference<T> out = getAdapter().advance(in.map(Polyfill::<O>uncheckedCast));

        if (item != null)
            executor.execute(() -> subStages.forEach(sub -> sub.accept(out.map(Object.class::cast))));
    }
}
