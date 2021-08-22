package org.comroid.mutatio.stack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class OutputStack<T> extends RefStack<T> {
    protected OutputStack(
            @Nullable RefStack<?> parent,
            @NotNull String name
    ) {
        super(parent, null, null, Overridability.NONE, name, parent == null ? 0 : parent.index(), null, null);
    }

    @Override
    protected abstract T $get();

    @Override
    protected boolean $set(T newValue) throws IllegalStateException {
        return false;
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
