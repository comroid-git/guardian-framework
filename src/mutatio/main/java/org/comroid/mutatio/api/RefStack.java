package org.comroid.mutatio.api;

import org.comroid.api.Index;
import org.comroid.api.MutableState;
import org.comroid.api.Named;
import org.comroid.api.Rewrapper;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RefStack<T> implements Rewrapper<T>, MutableState, Named, Index {
    private final AtomicReference<T> value;
    private final AtomicBoolean mutable;
    private final IntSupplier index;
    private final boolean isFinal;
    private final String name;
    protected @NotNull Supplier<? extends T> getter;
    protected @NotNull Predicate<? super T> setter;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isMutable() {
        return mutable.get();
    }

    public boolean isFinal() {
        return isFinal;
    }

    @Experimental
    protected RefStack(String name, T initialValue) {
        this(name, () -> -1, initialValue);
    }

    public RefStack(String name, IntSupplier index, T initialValue) {
        this(name, index, initialValue, true);
    }

    public RefStack(String name, IntSupplier index, T initialValue, boolean mutable) {
        this(name, index, initialValue, mutable, false);
    }

    public RefStack(String name, IntSupplier index, T initialValue, boolean mutable, boolean isFinal) {
        this.name = name;
        this.index = index;
        this.value = new AtomicReference<>(initialValue);
        this.mutable = new AtomicBoolean(mutable);
        this.isFinal = isFinal;
        this.getter = this::$get;
        this.setter = this::$set;
    }

    @Override
    public final boolean setMutable(boolean state) {
        mutable.set(state);
        return true;
    }

    public final int index() {
        return index.getAsInt();
    }

    public final T get() {
        return getter.get();
    }

    public final boolean set(T newValue) {
        return isMutable() && setter.test(newValue);
    }

    public final void resetGetter() throws IllegalStateException {
        replaceGetter(this::$get);
    }

    public final void replaceGetter(Supplier<? extends T> getter) throws IllegalStateException {
        if (isFinal())
            throw new IllegalStateException("RefStack " + getName() + " is Final; cannot override getter");
        this.getter = getter;
    }

    public final void resetSetter() throws IllegalStateException {
        replaceSetter(this::$set);
    }

    public final void replaceSetter(Predicate<? super T> setter) throws IllegalStateException {
        if (isFinal())
            throw new IllegalStateException("RefStack " + getName() + " is Final; cannot override setter");
        this.setter = setter;
    }

    private T $get() {
        return value.get();
    }

    private boolean $set(T newValue) throws IllegalStateException {
        if (isImmutable())
            throw new IllegalStateException("RefStack " + getName() + " is Immutable");
        value.set(newValue);
        return true;
    }
}
