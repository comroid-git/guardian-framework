package org.comroid.mutatio.ref;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.stack.RefStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.*;

/**
 * @deprecated needs fix
 */
@Deprecated // todo Needs fix
@SuppressWarnings("rawtypes")
public abstract class ParameterizedReference<P, T> extends ValueProvider<P, T> implements Ref<T>, Function<P, T> {
    private final Reference<P> defaultParameter = Reference.create();
    private RefStack[] stack = new RefStack[0];
    private Predicate<T> overriddenSetter;

    public final @Nullable P getDefaultParameter() {
        return defaultParameter.get();
    }

    @Override
    public boolean isMutable() {
        return overriddenSetter != null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final T get() throws ClassCastException {
        return putIntoCache((T) get(0));
    }

    protected ParameterizedReference(@Nullable ValueCache<?> parent, @Nullable Executor autocomputor) {
        super(parent, autocomputor);
    }

    public final boolean setDefaultParameter(P param) {
        return defaultParameter.set(param);
    }

    public final void defineSetter(Predicate<T> setter) {
        overriddenSetter = setter;
    }

    @Override
    public RefStack[] stack() {
        return stack;
    }

    @Override
    public void adjustStackSize(int newSize) {
        stack = Reference.$adjustStackSize(this, stack, newSize);
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
    public final <X, R> ParameterizedReference<X, R> addParameter(BiFunction<T, X, R> source) {
        throw new UnsupportedOperationException("Cannot add second Parameter");
    }

    @Override
    public final <X, R> ParameterizedReference<P, R> combine(Supplier<X> other, final BiFunction<T, X, R> accumulator) {
        throw new UnsupportedOperationException(); // todo
    }

    @Override
    public final ParameterizedReference<P, T> peek(Consumer<? super T> action) {
        return filter(Ref.wrapPeek(action));
    }

    @Override
    public final ParameterizedReference<P, T> filter(Predicate<? super T> predicate) {
        return new Support.Filtered<>(this, predicate, getExecutor());
    }

    @Override
    public final <R> ParameterizedReference<P, R> flatMap(final Class<R> type) {
        return filter(type::isInstance).map(type::cast);
    }

    @Override
    public <R> ParameterizedReference<P, R> map(Function<? super T, ? extends R> mapper){
        return new Support.Mapped<>(this, mapper, getExecutor());
    }

    @Override
    public final <R> ParameterizedReference<P, R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return map(mapper.andThen(Rewrapper::get));
    }

    @Override
    public final <R> ParameterizedReference<P, R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return flatMap(Ref.wrapOpt2Ref(mapper));
    }

    @Override
    public final ParameterizedReference<P, T> or(Supplier<? extends T> orElse) {
        throw new UnsupportedOperationException(); // todo
    }

    @Override
    public final T apply(P param) {
        return get(param);
    }

    public static final class Support {
        public static final class Source<I, P, O> extends ParameterizedReference<P, O> {
            private final BiFunction<I, P, O> function;

            public Source(
                    @NotNull Reference<I> parent,
                    @NotNull BiFunction<I, P, O> function
            ) {
                this(parent, function, parent.getExecutor());
            }

            public Source(
                    @NotNull Reference<I> parent,
                    @NotNull BiFunction<I, P, O> function,
                    @Nullable Executor autocomputor
            ) {
                super(parent, autocomputor);

                this.function = function;
            }

            @Override
            protected O doGet(P param) {
                //noinspection unchecked
                Reference<I> pref = (Reference<I>) Objects.requireNonNull(parent, "assertion");
                return pref.ifPresentMap(pv -> function.apply(pv, param));
            }
        }

        public static final class Filtered<P, T> extends ParameterizedReference<P, T> {
            private final ParameterizedReference<P, T> parent;
            private final Predicate<? super T> filter;

            public Filtered(
                    ParameterizedReference<P, T> parent,
                    Predicate<? super T> filter,
                    @Nullable Executor autocomputor
            ) {
                super(parent, autocomputor);
                this.parent = parent;
                this.filter = filter;
            }

            @Override
            protected T doGet(P param) {
                if (parent.test(filter))
                    return parent.get(param);
                return null;
            }
        }

        public static final class Mapped<P, I, O> extends ParameterizedReference<P, O> {
            private final Function<? super P, ? extends O> action;

            public Mapped(
                    ParameterizedReference<P, I> parent,
                    Function<? super I, ? extends O> mapper,
                    @Nullable Executor autocomputor
            ) {
                super(parent, autocomputor);
                this.action = parent.andThen(mapper);
            }

            @Override
            protected O doGet(P param) {
                return action.apply(param);
            }
        }
    }
}
