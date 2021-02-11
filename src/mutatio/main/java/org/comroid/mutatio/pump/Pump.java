package org.comroid.mutatio.pump;

import org.comroid.api.ExecutorBound;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.pipe.StageAdapter;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;

import java.util.Collection;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;

public interface Pump<T> extends Pipe<T>, Consumer<Reference<?>>, ExecutorBound {
    static <T> Pump<T> create() {
        return create(Runnable::run, Throwable::printStackTrace);
    }

    static <T> Pump<T> create(Consumer<Throwable> exceptionHandler) {
        return create(Runnable::run, exceptionHandler);
    }

    static <T> Pump<T> create(Executor executor) {
        return create(executor, throwable -> {
        });
    }

    static <T> Pump<T> create(Executor executor, Consumer<Throwable> exceptionHandler) {
        return new BasicPump<>(executor, ReferenceIndex.create(), exceptionHandler);
    }

    static <T> Pump<T> of(Collection<T> collection) {
        final Pump<T> pump = create();
        collection.stream()
                .map(Reference::constant)
                .map(ref -> ref.map(Object.class::cast))
                .forEach(pump);

        return pump;
    }

    static <T> Pump<T> combine(Stream<? extends Pipe<? extends T>> pipes) {
        final Pump<T>[] pumps = Polyfill.uncheckedCast(pipes.filter(Pump.class::isInstance).toArray(Pump[]::new));
        if (pumps.length == 0)
            throw new IllegalArgumentException("No valid Pumps found");
        if (pumps.length == 1)
            return pumps[0];
        final Pump<T> yield = create(pumps[0].getExceptionHandler());
        for (Pump<T> pump : pumps) {
            pump.forEach(it -> yield.accept(Reference.constant(it)));
            /*
            ((BasicPump<?, T>) pump).subStages.add(yield);
            pump.addDependent(yield);
             */
        }
        return yield;
        //throw new UnsupportedOperationException("");// todo check this
    }

    Consumer<Throwable> getExceptionHandler();

    @Override
    <R> Pump<R> addStage(StageAdapter<T, R> stage);

    <R> Pump<R> addStage(Executor executor, StageAdapter<T, R> stage);
}
