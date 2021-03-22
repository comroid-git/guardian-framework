package org.comroid.mutatio.model;

import org.comroid.api.Rewrapper;
import org.comroid.api.ValueBox;
import org.comroid.api.ValueType;
import org.comroid.mutatio.cache.SingleValueCache;
import org.comroid.mutatio.ref.ParameterizedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.*;

public interface Ref<T> extends SingleValueCache<T>, Rewrapper<T>, ValueBox<T> {
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
    @Internal
    static <T> Predicate<T> wrapPeek(Consumer<? super T> action) {
        return any -> {
            action.accept(any);
            return true;
        };
    }

    @NotNull
    @Internal
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
    <X, R> Ref<R> combine(Supplier<X> other, BiFunction<T, X, R> accumulator);

    <P, R> ParameterizedReference<P, R> addParameter(BiFunction<T, P, R> source);

    default Ref<T> peek(Consumer<? super T> action) {
        return filter(wrapPeek(action));
    }

    default boolean dependsOn(Ref<?> other) {
        return other.upstream().anyMatch(this::equals);
    }

    Ref<T> filter(Predicate<? super T> predicate);

    default <R> Ref<R> flatMap(Class<R> type) {
        return filter(type::isInstance).map(type::cast);
    }

    default <R> Ref<R> map(Function<? super T, ? extends R> mapper) {
        return map(mapper, null);
    }

    <R> Ref<R> map(Function<? super T, ? extends R> mapper, @Nullable Function<R, T> backwardsConverter);

    default <R> Ref<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return flatMap(mapper, null);
    }

    default <R> Ref<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper, @Nullable Function<R, T> backwardsConverter) {
        return map(mapper.andThen(Rewrapper::get), backwardsConverter);
    }

    default <R> Ref<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return flatMap(wrapOpt2Ref(mapper));
    }

    default <R> Ref<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper, @Nullable Function<R, T> backwardsConverter) {
        return flatMap(wrapOpt2Ref(mapper), backwardsConverter);
    }

    Ref<T> or(Supplier<T> orElse);
}
