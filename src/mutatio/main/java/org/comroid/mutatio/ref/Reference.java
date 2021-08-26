package org.comroid.mutatio.ref;

import org.comroid.annotations.Upgrade;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.SingleValueCache;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.model.ReferenceOverwriter;
import org.comroid.mutatio.stack.RefStack;
import org.comroid.mutatio.stack.RefStackUtil;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.*;

@SuppressWarnings("rawtypes")
public class Reference<T> extends ValueProvider.NoParam<T> implements Ref<T> {
    private static final Map<Object, Reference<?>> CONSTANTS = new ConcurrentHashMap<>();    private static final Reference<?> EMPTY = Reference.create(false, null);
    private final boolean mutable;
    private RefStack[] stack = new RefStack[0];

    @Internal
    @Deprecated
    protected <X> X getFromParent() {
        return getParent().into(ref -> ref.into(Polyfill::uncheckedCast));
    }

    @Override
    public boolean isMutable() {
        return mutable;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final T get() throws ClassCastException {
        return putIntoCache((T) get(0));
    }

    public final boolean set(T value) {
        if (set(0, value)) {
            putIntoCache(value);
            return true;
        }
        return false;
    }

    //region Constructors
    @Deprecated
    protected Reference(
            @Nullable ValueProvider.NoParam<?> parent,
            boolean mutable
    ) {
        this(parent, mutable, parent != null ? parent.getExecutor() : null);
    }

    @Deprecated
    protected Reference(
            @Nullable ValueProvider.NoParam<?> parent,
            boolean mutable,
            Executor autoComputor
    ) {
        this(parent, mutable, 1, autoComputor);
    }

    @Deprecated
    protected <X> Reference(
            final @Nullable Reference<X> parent
    ) {
        this(parent, parent != null ? parent.getExecutor() : null);
    }

    @Deprecated
    protected <X> Reference(
            final @Nullable Reference<X> parent,
            Executor autoComputor
    ) {
        this(parent, parent != null, 1, autoComputor);
    }

    @Deprecated
    private Reference(
            @Nullable ValueProvider.NoParam<?> parent,
            boolean mutable,
            int stackSize,
            Executor autoComputor
    ) {
        super(parent, autoComputor);

        this.mutable = mutable;
        adjustStackSize(stackSize);
    }

    public Reference() {
        this(true);
    }

    public Reference(boolean mutable) {
        this(null, null, mutable);
        adjustStackSize(1);
    }

    public Reference(int stackSize) {
        this(stackSize, true);
    }

    public Reference(int stackSize, boolean mutable) {
        this(null, null, mutable);
        adjustStackSize(stackSize);
    }

    public Reference(Supplier<T> getter, @Nullable Predicate<T> setter) {
        this(setter != null, new RefStack<>(null, "Reference(getter&setter)", 0, getter, setter));
    }

    public Reference(RefStack<?>... stack) {
        this(false, stack);
    }

    public Reference(boolean mutable, RefStack<?>... stack) {
        this(null, null, mutable, stack);
    }

    public Reference(
            @Nullable SingleValueCache<?> parent,
            @Nullable Executor autocomputor,
            boolean mutable,
            RefStack<?>... stack
    ) {
        super(parent, autocomputor);
        this.mutable = mutable;
        this.stack = stack == null ? new RefStack[0] : stack;
    }
    //endregion

    //region Static Methods
    public static <T> Reference<T> constant(@Nullable T of) {
        if (of == null)
            return empty();
        //noinspection unchecked
        return (Reference<T>) CONSTANTS.computeIfAbsent(of, v -> create(false, v));
    }

    public static <T> Reference<T> empty() {
        //noinspection unchecked
        return (Reference<T>) EMPTY;
    }

    public static <T> Reference<T> provided(Supplier<T> supplier) {
        return conditional(() -> true, supplier);
    }

    public static <T> Reference<T> conditional(BooleanSupplier condition, Supplier<T> supplier) {
        RefStack stack = RefStackUtil.$conditional(condition, supplier);
        return new Reference<>(null, null, false, stack);
    }

    public static <T> FutureReference<T> later(CompletableFuture<T> future) {
        return new FutureReference<>(future);
    }

    public static <T> Reference<T> create() {
        return create(null);
    }

    public static <T> Reference<T> create(@Nullable T initialValue) {
        return create(true, initialValue);
    }

    public static <T> Reference<T> create(boolean mutable, @Nullable T initialValue) {
        if (!mutable && initialValue == null && EMPTY != null)
            return empty();
        Reference<T> ref = new Reference<>(mutable);
        ref.set(initialValue);
        return ref;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Reference<T> optional(Optional<T> optional) {
        return provided(() -> optional.orElse(null));
    }

    public static <T> Reference<T> future(CompletableFuture<T> future) {
        return new Reference<>(RefStackUtil.$future(future));
    }

    @Upgrade
    public static <T> Reference<T> upgrade(RefStack<T> stack) {
        return new Reference<>(stack);
    }

    @Upgrade
    public static <T> Reference<T> upgrade(Rewrapper<T> rewrapper) {
        return provided(rewrapper);
    }
    //endregion

    //region RefStack Methods
    static RefStack[] $adjustStackSize(Ref ref, RefStack[] stack, int newSize) {
        int oldSize = stack.length;
        stack = Arrays.copyOf(stack, newSize);
        for (int i = 0; i < stack.length; i++)
            if (stack[i] == null)
                stack[i] = new RefStack<>(ref.getName() + '#' + i, i, null, ref.isMutable(),
                        ref.isMutable() ? RefStack.Overridability.GETTER_AND_SETTER : RefStack.Overridability.GETTER);
            else if (stack[i].index() != i)
                // fixme: not expected; but here's a temporary error for you:
                throw new IllegalStateException("RefStack order of " + ref + " is incorrect");
        return stack;
    }

    @Override
    public RefStack[] stack() {
        return stack;
    }

    @Override
    public void adjustStackSize(int newSize) {
        stack = $adjustStackSize(this, stack, newSize);
    }
    //endregion

    //region RefOP Methods
    @Override
    public final Reference<T> peek(Consumer<? super T> action) {
        return map(it -> {
            action.accept(it);
            return it;
        });
    }

    @Override
    public final <X, R> Reference<R> combine(final Supplier<X> other, final BiFunction<T, X, R> accumulator) {
        RefStack stack = RefStackUtil.$combine(stack(0, false), other, accumulator);
        return new Reference<>(this, getExecutor(), false, stack);
    }

    @Override
    public final <P, R> ParameterizedReference<P, R> addParameter(BiFunction<T, P, R> source) {
        return new ParameterizedReference.Support.Source<>(this, source);
    }

    @Override
    @Deprecated
    public final void rebind(final Supplier<T> behind) {
        if (behind == this || (behind instanceof Reference && dependsOn((Reference<T>) behind)))
            throw new IllegalArgumentException("Circular Reference Dependency detected");

        overriddenSupplier = nil -> behind.get();
        outdateCache();
    }

    @Override
    public final Reference<T> filter(Predicate<? super T> predicate) {
        RefStack stack = RefStackUtil.$filter(stack(0, false), predicate);
        return new Reference<>(this, getExecutor(), false, stack);
    }

    @Override
    public final <R> Reference<R> map(Function<? super T, ? extends R> mapper) {
        RefStack stack = RefStackUtil.$map(stack(0, false), mapper);
        return new Reference<>(this, getExecutor(), false, stack);
    }

    @Override
    public final <R> Reference<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return map(mapper.andThen(it -> it.orElse(null)));
    }

    @Override
    public final Reference<T> or(Supplier<? extends T> orElse) {
        RefStack stack = RefStackUtil.$or(stack(0, false), orElse);
        return new Reference<>(this, getExecutor(), false, stack);
    }
    //endregion

    @Override
    public final String toString() {
        return ifPresentMapOrElseGet(String::valueOf, () -> "null");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Reference && (contentEquals(((Reference<?>) other).get()) || other == this);
    }

    //region ValueCache Methods
    @Override
    protected final T doGet() {
        //noinspection unchecked
        return (T) get(0);
    }

    @OverrideOnly
    protected final boolean doSet(T value) {
        return set(0, putIntoCache(value));
    }
    //endregion

    public interface Advancer<I, O> extends ReferenceOverwriter<I, O, Reference<I>, Reference<O>> {
        Reference<O> advance(Reference<I> ref);
    }


}
