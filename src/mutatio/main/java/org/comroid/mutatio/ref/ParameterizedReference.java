package org.comroid.mutatio.ref;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executor;
import java.util.function.Function;

public final class ParameterizedReference<P, T> extends ValueProvider<P, T> implements Function<P, T> {
    protected ParameterizedReference(@Nullable ValueProvider<?, ?> parent, @Nullable Executor autocomputor) {
        super(parent, autocomputor);
    }

    @Override
    public T apply(P param) {
        return get(param);
    }

    @Override
    protected T doGet(P param) {
        return null;
    }
}
