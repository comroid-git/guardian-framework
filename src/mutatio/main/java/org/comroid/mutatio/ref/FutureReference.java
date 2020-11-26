package org.comroid.mutatio.ref;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class FutureReference<T> extends Reference.Support.Base<T> {
    public final CompletableFuture<T> future;

    public FutureReference() {
        this(new CompletableFuture<>());
    }

    public FutureReference(CompletableFuture<T> future) {
        super(null, false);

        this.future = future;
    }

    @Nullable
    @Override
    protected T doGet() {
        return future.join();
    }

    @Override
    protected boolean doSet(T value) {
        if (future.isDone())
            return false;
        future.complete(value);
        return true;
    }

    public void complete(T value) {
        if (future.isDone())
            throw new IllegalStateException("Future is already done");
        future.complete(value);
    }
}
