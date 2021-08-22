package org.comroid.mutatio.stack;

import org.comroid.api.*;
import org.comroid.mutatio.cache.SingleValueCache;
import org.comroid.mutatio.cache.ValueCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class RefStack<T> extends SingleValueCache.Abstract<T> implements Rewrapper<T>, MutableState, Named, Index, Upgradeable<RefStack<? super T>> {
    private final AtomicReference<T> value;
    private final AtomicBoolean mutable;
    private final int overridable;
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

    public boolean isGetterOverridable() {
        return Overridability.GETTER.isFlagSet(overridable);
    }

    public boolean isSetterOverridable() {
        return Overridability.SETTER.isFlagSet(overridable);
    }

    public RefStack(
            String name,
            int index,
            T initialValue,
            boolean mutable
    ) {
        this(name, index, initialValue, mutable, Overridability.GETTER_AND_SETTER);
    }

    public RefStack(
            String name,
            int index,
            T initialValue,
            boolean mutable,
            Overridability overridable
    ) {
        this(null, name, index, initialValue, mutable, overridable);
    }

    public RefStack(
            @Nullable ValueCache<?> parent,
            String name,
            int index,
            T initialValue,
            boolean mutable
    ) {
        this(parent, name, index, initialValue, mutable, Overridability.GETTER);
    }

    public RefStack(
            @Nullable ValueCache<?> parent,
            String name,
            int index,
            T initialValue,
            boolean mutable,
            Overridability overridable
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
            Overridability overridable,
            String name,
            int index,
            @Nullable Supplier<? extends T> getter,
            @Nullable Predicate<? super T> setter
    ) {
        super(parent, null);
        this.value = value;
        this.mutable = mutable;
        this.overridable = overridable.getAsInt();
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
        if (isUpToDate())
            return getFromCache();
        return putIntoCache(getter.get());
    }

    public final boolean set(T newValue) {
        if (!isMutable() || !setter.test(newValue))
            return false;
        putIntoCache(newValue);
        return true;
    }

    public final void resetGetter() throws IllegalStateException {
        overrideGetter(null);
    }

    public final void overrideGetter(Supplier<? extends T> getter) throws IllegalStateException {
        if (isGetterOverridable())
            throw new IllegalStateException("RefStack " + getName() + " is Final; cannot override getter");
        if (getter == this || (getter instanceof ValueCache && dependsOn((ValueCache<?>) getter)))
            throw new IllegalArgumentException("Circular RefStack Dependency detected");
        this.getter = Polyfill.<Supplier<? extends T>>notnullOr(getter, this::$get);
    }

    public final void resetSetter() throws IllegalStateException {
        overrideSetter(null);
    }

    public final void overrideSetter(Predicate<? super T> setter) throws IllegalStateException {
        if (isSetterOverridable())
            throw new IllegalStateException("RefStack " + getName() + " is Final; cannot override setter");
        this.setter = Polyfill.<Predicate<? super T>>notnullOr(setter, this::$set);
    }

    protected T $get() {
        return value.get();
    }

    protected boolean $set(T newValue) throws IllegalStateException {
        if (isImmutable())
            throw new IllegalStateException("RefStack " + getName() + " is Immutable");
        value.set(newValue);
        return true;
    }

    @Override
    public void computeAndStoreValue() {
        putIntoCache(get());
    }

    public enum Overridability implements BitmaskAttribute<Overridability> {
        NONE, // 0
        GETTER, // 1
        SETTER, // 2
        GETTER_AND_SETTER // 4
    }
}
