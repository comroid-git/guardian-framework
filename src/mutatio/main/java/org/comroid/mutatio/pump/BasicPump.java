package org.comroid.mutatio.pump;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.mutatio.pipe.impl.BasicPipe;
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

    public BasicPump(Executor executor, ReferenceIndex<O> old, StageAdapter<O, T, Reference<O>, Reference<T>> adapter) {
        super(old, adapter, 50);

        this.executor = executor;
    }

    @Override
    public <R> Pump<R> addStage(StageAdapter<T, R, Reference<T>, Reference<R>> stage) {
        return addStage(executor, stage);
    }

    @Override
    public <R> Pump<R> addStage(Executor executor, StageAdapter<T, R, Reference<T>, Reference<R>> stage) {
        BasicPump<T, R> sub = new BasicPump<>(executor, this, stage);
        subStages.add(sub);
        return sub;
    }

    @Override
    public void accept(final Reference<?> in) {
        final Reference<T> out = getAdapter().advance(in);

        if (!(refs instanceof Pump))
            add(out.get());

        // and then all substages
        executor.execute(() -> subStages.forEach(sub -> {
            try {
                sub.accept(out);
            } catch (Throwable t) {
                logger.error("Unhandled exception in Pump", t);
            }
        }));
        // compute this once if hasnt already
        if (out.isOutdated())
            out.get();
    }

    private static final Logger logger = LogManager.getLogger();
}
