package org.comroid.mutatio.ref;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @deprecated Use {@link Reference#future(CompletableFuture)}
 */
@Deprecated
public final class FutureReference<T> extends Reference<T> {
    public final CompletableFuture<T> future;

    public FutureReference() {
        this(new CompletableFuture<>());
    }

    public FutureReference(CompletableFuture<T> future) {
        super(null, false);

        System.err.println("Warning: The FutureReference class does not work any longer! Please use Reference.future()");

        this.future = future;
    }
}
