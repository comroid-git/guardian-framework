package org.comroid.mutatio.ref;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.SingleValueCache;
import org.comroid.mutatio.cache.ValueUpdateListener;
import org.jetbrains.annotations.Contract;

import java.util.Optional;
import java.util.function.*;

public interface Ref<T> extends SingleValueCache<T>, Rewrapper<T> {
    @Override
    boolean isMutable();

    @Deprecated
    boolean isPresent();

    Reference<T> peek(Consumer<? super T> action);

    @Contract("-> this")
    @Deprecated
    Reference<T> process();

    boolean unset();

    @Override
    <X, R> Reference<R> combine(Supplier<X> other, BiFunction<T, X, R> accumulator);

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

    ValueUpdateListener<T> apply(Consumer<T> action);

    Reference<T> filter(Predicate<? super T> predicate);

    <R> Reference<R> flatMap(Class<R> type);

    <R> Reference<R> map(Function<? super T, ? extends R> mapper);

    <R> Reference<R> flatMap(Function<? super T, ? extends Reference<? extends R>> mapper);

    <R> Reference<R> flatMap(Function<? super T, ? extends Reference<? extends R>> mapper, Function<R, T> backwardsConverter);

    <R> Reference<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper);

    <R> Reference<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper, Function<R, T> backwardsConverter);

    Reference<T> or(Supplier<T> orElse);
}
