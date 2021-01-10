package org.comroid.uniform.model;

import org.comroid.api.HeldType;
import org.comroid.uniform.ValueType;

public abstract class ValueAdapter<B> {
    protected final B base;
    protected final ValueType<B> actualType;

    protected ValueAdapter(B base, ValueType<B> actualType) {
        this.base = base;
        this.actualType = actualType;
    }

    public final <T> T asType(HeldType<T> type) {
        return actualType.convert(asActualType(), type);
    }

    protected abstract B asActualType();
}
