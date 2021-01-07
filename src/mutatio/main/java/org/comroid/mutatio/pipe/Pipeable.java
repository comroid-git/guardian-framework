package org.comroid.mutatio.pipe;

import java.util.Collection;

import static org.jetbrains.annotations.ApiStatus.OverrideOnly;

public interface Pipeable<T> {
    Pipe<? extends T> pipe();

    interface From<T> extends Pipeable<T> {
        @Override
        default Pipe<? extends T> pipe() {
            return Pipe.of(fetchPipeContent());
        }

        @OverrideOnly
        Collection<? extends T> fetchPipeContent();
    }
}
