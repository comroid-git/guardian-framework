package org.comroid.mutatio.pump;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.mutatio.pipe.impl.BasicPipe;
import org.comroid.mutatio.pipe.StageAdapter;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class BasicPump<O, T> extends BasicPipe<O, T> implements Pump<T> {
    private final Collection<Pump<?>> subStages = new ArrayList<>();
    private final Consumer<Throwable> exceptionHandler;
    private final Executor executor;

    @Override
    public Executor getExecutor() {
        return executor;
    }

    public BasicPump(ReferenceIndex<O> old, Consumer<Throwable> exceptionHandler) {
        this(Runnable::run, old, exceptionHandler);
    }

    public BasicPump(Executor executor, ReferenceIndex<O> old, Consumer<Throwable> exceptionHandler) {
        //noinspection unchecked
        this(executor, old, StageAdapter.map(it -> (T) it), exceptionHandler);
    }

    public BasicPump(
            Executor executor,
            ReferenceIndex<O> old,
            StageAdapter<O, T, Reference<O>, Reference<T>> adapter,
            Consumer<Throwable> exceptionHandler
    ) {
        super(old, adapter, 50);

        this.executor = executor;
        this.exceptionHandler = new Consumer<Throwable>() {
            private final Set<Throwable> distinct = new HashSet<>();

            @Override
            public void accept(Throwable throwable) {
                if (distinct.add(throwable))
                    exceptionHandler.accept(throwable);
            }
        };
    }

    @Override
    public <R> Pump<R> addStage(StageAdapter<T, R, Reference<T>, Reference<R>> stage) {
        return addStage(executor, stage);
    }

    @Override
    public <R> Pump<R> addStage(Executor executor, StageAdapter<T, R, Reference<T>, Reference<R>> stage) {
        BasicPump<T, R> sub = new BasicPump<>(executor, this, stage, exceptionHandler);
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
                exceptionHandler.accept(t);
            }
        }));
        // compute this once if hasnt already
        if (out.isOutdated())
            out.get();
    }

    private static final Logger logger = LogManager.getLogger();
}
