package org.comroid.mutatio.model;

import org.comroid.api.Index;
import org.comroid.api.Rewrapper;
import org.comroid.api.ValueBox;
import org.comroid.api.ValueType;
import org.comroid.mutatio.api.RefStack;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.ref.ParameterizedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.*;

public interface Ref<T> extends ValueCache<T>, Rewrapper<T>, ValueBox<T>, Index {
    //region RefStack Methods

    /**
     * Returns the index of this Reference.
     * Returns {@code -1} if there is no index.
     *
     * @return the index of this Reference
     */
    @Override
    default int index() {
        return -1;
    }

    /**
     * Returns the full Reference Stack underlying this Reference.
     *
     * @return the full Reference stack
     */
    @Internal
    @SuppressWarnings("rawtypes")
    RefStack[] stack();

    @Internal
    void adjustStackSize(int newSize);

    @Internal
    @SuppressWarnings("rawtypes")
    default RefStack stack(int index, boolean createIfAbsent) throws IndexOutOfBoundsException {
        RefStack[] stack = stack();
        if (index >= stack.length || index < 0) {
            if (createIfAbsent) adjustStackSize(index + 1);
            else
                throw new IndexOutOfBoundsException(String.format("Stack index %d is out of bounds; length = %d", index, stack.length));
        }
        return stack[index];
    }

    /**
     * Returns the Value held by the given index.
     *
     * @param stack The index of the value.
     * @return the Value
     */
    @Internal
    default Object get(int stack) throws IndexOutOfBoundsException {
        return stack(stack, false).get();
    }

    @Internal
    default boolean set(int stack, Object value) throws IndexOutOfBoundsException {
        //noinspection unchecked
        return stack(stack, true).set(value);
    }
    //endregion

    //region Accessor Methods
    @Override
    @SuppressWarnings("unchecked")
    default T get() throws ClassCastException {
        return (T) get(0);
    }

    default boolean set(T value) {
        return set(0, value);
    }

    default boolean unset() {
        return set(null);
    }

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
    //endregion

    //region ValueBox Methods
    @Override
    default T getValue() {
        return get();
    }

    @Override
    @Nullable
    default ValueType<? extends T> getHeldType() {
        return StandardValueType.typeOf(getValue());
    }
    // endregion

    //region RefOPs Methods
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

    <R> Ref<R> map(Function<? super T, ? extends R> mapper);

    default <R> Ref<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return map(mapper.andThen(Rewrapper::get));
    }

    default <R> Ref<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return flatMap(wrapOpt2Ref(mapper));
    }
    //endregion

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

    /**
     * @deprecated Use {@link RefStack#overrideGetter(Supplier)}
     */
    @Deprecated
    void rebind(Supplier<T> behind);
}
