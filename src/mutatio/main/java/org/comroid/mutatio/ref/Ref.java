package org.comroid.mutatio.ref;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.SingleValueCache;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.*;

public interface Ref<T> extends SingleValueCache<T>, Rewrapper<T> {
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
        if (!set(into(computor)))
            throw new UnsupportedOperationException("Could not set value");
        return get();
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    default T computeIfPresent(Function<T, T> computor) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        if (!isNull() && !set(into(computor)))
            throw new UnsupportedOperationException("Could not set value");
        return get();
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    default T computeIfAbsent(Supplier<T> supplier) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        if (isNull() && !set(supplier.get()))
            throw new UnsupportedOperationException("Could not set value");
        return get();
    }

    default T getAndCompute(Function<T, T> computor) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        final T old = get();
        if (!set(into(computor)))
            throw new UnsupportedOperationException("Could not set value");
        return old;
    }

    void rebind(Supplier<T> behind);

    @Override
    <X, R> Ref<R> combine(Supplier<X> other, BiFunction<T, X, R> accumulator);

    <P, R> ParameterizedReference<P, R> addParameter(BiFunction<T, P, R> source);

    default Ref<T> peek(Consumer<? super T> action) {
        return filter(wrapPeek(action));
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
