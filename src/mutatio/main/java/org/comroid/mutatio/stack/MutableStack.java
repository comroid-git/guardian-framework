package org.comroid.mutatio.stack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class MutableStack<T> extends OutputStack<T> {
    protected MutableStack(@Nullable RefStack<?> parent, @NotNull String name) {
        super(parent, name);
    }

    protected abstract boolean $set(T value);

    @Override
    public boolean isMutable() {
        return true;
    }
}
