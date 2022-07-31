package org.comroid.mutatio.stack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MutableStack<T> extends OutputStack<T> {
    @Override
    public boolean isMutable() {
        return true;
    }

    protected MutableStack(@Nullable RefStack<?> parent, @NotNull String name) {
        super(parent, name);
    }

    protected abstract boolean $set(T value);
}
