package org.comroid.mutatio.model;

import org.comroid.api.Rewrapper;
import org.comroid.api.ValueBox;
import org.comroid.api.ValueType;
import org.comroid.mutatio.ref.ParameterizedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.*;

public interface BaseRef<T> extends Rewrapper<T>, ValueBox<T> {
    @Override
    default T getValue() {
        return get();
    }

    @Override
    @Nullable
    default ValueType<? extends T> getHeldType() {
        return StandardValueType.typeOf(getValue());
    }

    @Override
    boolean isMutable();

    @NotNull
    @ApiStatus.Internal
    static <T> Predicate<T> wrapPeek(Consumer<? super T> action) {
        return any -> {
            action.accept(any);
            return true;
        };
    }

    @NotNull
    @ApiStatus.Internal
    static <T, R> Function<? super T, ? extends Reference<? extends R>> wrapOpt2Ref(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return mapper.andThen(opt -> opt.map(Reference::constant).orElseGet(Reference::empty));
    }

    @Override
    T get();

    default boolean unset() {
        return set(null);
    }

    boolean set(T value);

    default T replace(T newValue) {
        T old = get();
        if (set(newValue))
            return old;
        return null;
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    default T compute(Function<T, T> computor) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        set(into(computor));
        return get();
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    default T computeIfPresent(Function<T, T> computor) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        if (!isNull()) set(into(computor));
        return get();
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    default T computeIfAbsent(Supplier<T> supplier) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        if (isNull()) set(supplier.get());
        return get();
    }

    default T getAndCompute(Function<T, T> computor) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        final T old = get();
        set(into(computor));
        return old;
    }

    void rebind(Supplier<T> behind);

    @Override
    <X, R> BaseRef<R> combine(Supplier<X> other, BiFunction<T, X, R> accumulator);

    <P, R> ParameterizedReference<P, R> addParameter(BiFunction<T, P, R> source);

    default BaseRef<T> peek(Consumer<? super T> action) {
        return filter(wrapPeek(action));
    }

    BaseRef<T> filter(Predicate<? super T> predicate);

    default <R> BaseRef<R> flatMap(Class<R> type) {
        return filter(type::isInstance).map(type::cast);
    }

    default <R> BaseRef<R> map(Function<? super T, ? extends R> mapper) {
        return map(mapper, null);
    }

    <R> BaseRef<R> map(Function<? super T, ? extends R> mapper, @Nullable Function<R, T> backwardsConverter);

    default <R> BaseRef<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return flatMap(mapper, null);
    }

    default <R> BaseRef<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper, @Nullable Function<R, T> backwardsConverter) {
        return map(mapper.andThen(Rewrapper::get), backwardsConverter);
    }

    default <R> BaseRef<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return flatMap(wrapOpt2Ref(mapper));
    }

    default <R> BaseRef<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper, @Nullable Function<R, T> backwardsConverter) {
        return flatMap(wrapOpt2Ref(mapper), backwardsConverter);
    }
}
