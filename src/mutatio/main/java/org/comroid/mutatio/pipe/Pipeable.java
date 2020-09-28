package org.comroid.mutatio.pipe;

import org.comroid.mutatio.pump.Pump;

import java.util.Collection;
import java.util.concurrent.Executor;

import static org.jetbrains.annotations.ApiStatus.OverrideOnly;

public interface Pipeable<T> {
    Pipe<? extends T> pipe();

    default Pump<? extends T> pump() {
        return pump(Runnable::run);
    }

    Pump<? extends T> pump(Executor executor);

    interface From<T> extends Pipeable<T> {
        @Override
        default Pipe<? extends T> pipe() {
            return Pipe.of(fetchPipeContent());
        }

        @OverrideOnly
        Collection<? extends T> fetchPipeContent();
    }
}
