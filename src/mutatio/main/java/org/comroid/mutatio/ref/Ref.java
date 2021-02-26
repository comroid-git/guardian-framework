package org.comroid.mutatio.ref;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.SingleValueCache;
import org.jetbrains.annotations.Contract;

import java.util.Optional;
import java.util.function.*;

public interface Ref<T> extends SingleValueCache<T>, Rewrapper<T> {
    @Override
    boolean isMutable();

    @Deprecated
    boolean isPresent();

    @Override
    T get();

    Ref<T> peek(Consumer<? super T> action);

    @Contract("-> this")
    @Deprecated
    Ref<T> process();

    boolean unset();

    @Override
    <X, R> Ref<R> combine(Supplier<X> other, BiFunction<T, X, R> accumulator);

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

    Ref<T> filter(Predicate<? super T> predicate);

    <R> Ref<R> flatMap(Class<R> type);

    <R> Ref<R> map(Function<? super T, ? extends R> mapper);

    <R> Ref<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper);

    <R> Ref<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper, Function<R, T> backwardsConverter);

    <R> Ref<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper);

    <R> Ref<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper, Function<R, T> backwardsConverter);

    Ref<T> or(Supplier<T> orElse);
}
