package org.comroid.mutatio.ref;

import org.comroid.api.Rewrapper;
import org.comroid.api.TriFunction;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.*;

public final class ParameterizedReference<P, T> extends ValueProvider<P, T> implements Ref<T>, Function<P, T> {
    private final Reference<P> defaultParameter = Reference.create();
    private Predicate<T> overriddenSetter;

    public final @Nullable P getDefaultParameter() {
        return defaultParameter.get();
    }

    @Override
    public boolean isMutable() {
        return false; // todo
    }

    protected ParameterizedReference(@Nullable ValueProvider<?, ?> parent, @Nullable Executor autocomputor) {
        super(parent, autocomputor);
    }

    public final boolean setDefaultParameter(P param) {
        return defaultParameter.set(param);
    }

    @Override
    public T get() {
        return defaultParameter.ifPresentMapOrElseThrow(this::get,
                () -> new NoSuchElementException("No default parameter defined"));
    }

    @Override
    public boolean unset() {
        return false; // todo
    }

    @Override
    public boolean set(T value) {
        return false; // todo
    }

    @Override
    public final void rebind(Supplier<T> behind) {
        throw new UnsupportedOperationException("Cannot rebind ParameterizedReference with Supplier");
    }

    public final void rebind(Function<P, T> behind) {
        if (behind == this || (behind instanceof Reference
                && ((Reference<T>) behind).upstream().noneMatch(this::equals)))
            throw new IllegalArgumentException("Cannot rebind behind itself");

        overriddenSupplier = behind;
        if (behind instanceof Reference)
            overriddenSetter = ((Reference<T>) behind)::set;
        outdateCache();
    }

    @Override
    public final <X, R> Ref<R> combine(Supplier<X> other, final BiFunction<T, X, R> accumulator) {
        return combine(other, (p1, p2, p3) -> accumulator.apply(p2, p3));
    }

    public final <X, R> ParameterizedReference<P, R> combine(Supplier<X> other, TriFunction<P, T, X, R> accumulator) {
        return null; // todo
    }

    @Override
    public ParameterizedReference<P, T> peek(Consumer<? super T> action) {
        return null; // todo
    }

    @Override
    public ParameterizedReference<P, T> filter(Predicate<? super T> predicate) {
        return null; // todo
    }

    @Override
    public <R> ParameterizedReference<P, R> flatMap(Class<R> type) {
        return null; // todo
    }

    @Override
    public <R> ParameterizedReference<P, R> map(Function<? super T, ? extends R> mapper) {
        return null; // todo
    }

    @Override
    public <R> ParameterizedReference<P, R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return null; // todo
    }

    @Override
    public <R> ParameterizedReference<P, R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper, Function<R, T> backwardsConverter) {
        return null; // todo
    }

    @Override
    public <R> ParameterizedReference<P, R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return null; // todo
    }

    @Override
    public <R> ParameterizedReference<P, R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper, Function<R, T> backwardsConverter) {
        return null; // todo
    }

    @Override
    public ParameterizedReference<P, T> or(Supplier<T> orElse) {
        return null; // todo
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
