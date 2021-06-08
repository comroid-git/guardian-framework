package org.comroid.mutatio.model;

import org.comroid.api.Rewrapper;
import org.comroid.api.ValueProvider;
import org.comroid.mutatio.ref.ParameterizedReference;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.*;

@Experimental
public interface ParamRef<P, O> extends BaseRef<O>, ValueProvider<P, O> {
    default boolean set(P param, O value) {
        throw new UnsupportedOperationException("Cannot set");
    }

    default boolean unset(P param) {
        return set(param, null);
    }

    @Override
    default void rebind(Supplier<O> behind) {
        throw new UnsupportedOperationException("Can not rebind");
    }

    @Override
    @NonExtendable
    default O get() {
        throw new AbstractMethodError("Cannot call get() without parameter!");
    }

    @Override
    @NonExtendable
    default boolean set(O value) {
        throw new AbstractMethodError("Cannot call set() without parameter!");
    }

    @Override
    default <X, R> ParamRef<P, R> combine(Supplier<X> other, BiFunction<O, X, R> accumulator) {
        throw new UnsupportedOperationException("todo"); // todo
    }

    @Override
    @NonExtendable
    default <x, R> ParameterizedReference<x, R> addParameter(BiFunction<O, x, R> source) {
        throw new AbstractMethodError("Cannot add parameter; parameter is already defined!");
    }

    @Override
    default ParamRef<P, O> peek(Consumer<? super O> action) {
        return filter(BaseRef.wrapPeek(action));
    }

    @Override
    default ParamRef<P, O> filter(Predicate<? super O> predicate) {
        return new ParameterizedReference.Support.Filtered<>(this, predicate, null);
    }

    @Override
    default <R> ParamRef<P, R> flatMap(final Class<R> type) {
        return filter(type::isInstance).map(type::cast);
    }

    @Override
    default <R> ParamRef<P, R> map(Function<? super O, ? extends R> mapper) {
        return map(mapper, null);
    }

    @Override
    default <R> ParamRef<P, R> map(Function<? super O, ? extends R> mapper, @Nullable Function<R, O> backwardsConverter) {
        return new ParameterizedReference.Support.Mapped<P, O, R>(this, mapper, backwardsConverter, null);
    }

    default <R> ParamRef<P, R> mapBoth(BiFunction<? super P, ? super O, ? extends R> mapper) {
        // todo
    }

    @Override
    default <R> ParamRef<P, R> flatMap(Function<? super O, ? extends Rewrapper<? extends R>> mapper) {
        return flatMap(mapper, null);
    }

    @Override
    default <R> ParamRef<P, R> flatMap(Function<? super O, ? extends Rewrapper<? extends R>> mapper, Function<R, O> backwardsConverter) {
        return map(mapper.andThen(Rewrapper::get), backwardsConverter);
    }

    @Override
    default <R> ParamRef<P, R> flatMapOptional(Function<? super O, ? extends Optional<? extends R>> mapper) {
        return flatMapOptional(mapper, null);
    }

    @Override
    default <R> ParamRef<P, R> flatMapOptional(Function<? super O, ? extends Optional<? extends R>> mapper, Function<R, O> backwardsConverter) {
        return flatMap(BaseRef.wrapOpt2Ref(mapper), backwardsConverter);
    }
}
