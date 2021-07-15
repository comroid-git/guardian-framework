package org.comroid.mutatio.model;

import org.comroid.api.Index;
import org.comroid.api.MutableState;
import org.comroid.api.Rewrapper;

import java.util.function.Supplier;

public interface RefStack<T> extends Rewrapper<T>, MutableState, Index {
    int index();

    T get();

    boolean set(T newValue);

    boolean rebind(Supplier<? extends T> newSupplier);
}
