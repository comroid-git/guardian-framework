package org.comroid.mutatio.ref;

import java.util.function.Function;

public final class ParameterizedReference<P, T> extends Reference<T> implements Function<P, T> {
    @Override
    protected T doGet() {
        return apply((P) null);
    }

    @Override
    public T apply(P p) {
        return null;
    }
}
