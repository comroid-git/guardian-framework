package org.comroid.mutatio.api;

import org.comroid.api.*;
import org.comroid.mutatio.cache.SingleValueCache;
import org.comroid.mutatio.cache.ValueCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RefStack<T> extends SingleValueCache.Abstract<T> implements Rewrapper<T>, MutableState, Named, Index {
    private final AtomicReference<T> value;
    private final AtomicBoolean mutable;
    private final boolean overridable;
    private final String name;
    private final int index;
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

    public boolean isOverridable() {
        return overridable;
    }

    public RefStack(
            @Nullable ValueCache<?> parent,
            String name,
            int index,
            T initialValue,
            boolean mutable,
            boolean overridable
    ) {
        this(
                parent,
                new AtomicReference<>(initialValue),
                new AtomicBoolean(mutable),
                overridable,
                name,
                index,
                null,
                null
        );
    }

    protected RefStack(
            @Nullable ValueCache<?> parent,
            AtomicReference<T> value,
            AtomicBoolean mutable,
            boolean overridable,
            String name,
            int index,
            @Nullable Supplier<? extends T> getter,
            @Nullable Predicate<? super T> setter
    ) {
        super(parent, null);
        this.value = value;
        this.mutable = mutable;
        this.overridable = overridable;
        this.name = name;
        this.index = index;
        this.getter = Polyfill.<Supplier<? extends T>>notnullOr(getter, this::$get);
        this.setter = Polyfill.<Predicate<? super T>>notnullOr(setter, this::$set);
    }

    @Override
    public final boolean setMutable(boolean state) {
        mutable.set(state);
        return true;
    }

    public final int index() {
        return index;
    }

    public final T get() {
        return getter.get();
    }

    public final boolean set(T newValue) {
        return isMutable() && setter.test(newValue);
    }

    public final void resetGetter() throws IllegalStateException {
        overrideGetter(null);
    }

    public final void overrideGetter(Supplier<? extends T> getter) throws IllegalStateException {
        if (isOverridable())
            throw new IllegalStateException("RefStack " + getName() + " is Final; cannot override getter");
        this.getter = Polyfill.<Supplier<? extends T>>notnullOr(getter, this::$get);
    }

    public final void resetSetter() throws IllegalStateException {
        overrideSetter(null);
    }

    public final void overrideSetter(Predicate<? super T> setter) throws IllegalStateException {
        if (isOverridable())
            throw new IllegalStateException("RefStack " + getName() + " is Final; cannot override setter");
        this.setter = Polyfill.<Predicate<? super T>>notnullOr(setter, this::$set);
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

    @Override
    public void computeAndStoreValue() {
        putIntoCache(get());
    }
}
