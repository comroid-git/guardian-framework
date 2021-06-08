package org.comroid.mutatio.ref;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.model.ParamRef;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.model.BaseRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.*;

public abstract class ParameterizedReference<P, T> implements ParamRef<P, T> {
    private final Reference<P> defaultParameter = Reference.create();
    private Function<P, T> overriddenSupplier;
    private Predicate<T> overriddenSetter;

    public final @Nullable P getDefaultParameter() {
        return defaultParameter.get();
    }

    @Override
    public boolean isMutable() {
        return overriddenSetter != null;
    }

    public final boolean setDefaultParameter(P param) {
        return defaultParameter.set(param);
    }

    public final void defineSetter(Predicate<T> setter) {
        overriddenSetter = setter;
    }

    @Override
    public final T get() {
        return defaultParameter.ifPresentMapOrElseThrow(this::get,
                () -> new NoSuchElementException("No default parameter defined"));
    }

    @Override
    public boolean set(T value) {
        return overriddenSetter.test(value);
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
    }

    public static final class Support {
        public static final class Source<I, P, O> extends ParameterizedReference<P, O> {
            private final Reference<I> parent;
            private final BiFunction<I, P, O> function;
            private final Executor autocomputor;

            public Source(
                    @NotNull Reference<I> parent,
                    @NotNull BiFunction<I, P, O> function
            ) {
                this(parent, function, parent.getAutocomputor());
            }

            public Source(
                    @NotNull Reference<I> parent,
                    @NotNull BiFunction<I, P, O> function,
                    @Nullable Executor autocomputor
            ) {
                this.parent = parent;
                this.function = function;
                this.autocomputor = autocomputor;
            }

            @Override
            public O get(P param) {
                Reference<I> pref = Objects.requireNonNull(parent, "assertion");
                return pref.ifPresentMap(pv -> function.apply(pv, param));
            }
        }

        public static final class Filtered<P, T> extends ParameterizedReference<P, T> {
            private final ParamRef<P, T> parent;
            private final Predicate<? super T> filter;
            private final Executor autocomputor;

            public Filtered(
                    ParamRef<P, T> parent,
                    Predicate<? super T> filter,
                    @Nullable Executor autocomputor
            ) {
                this.parent = parent;
                this.filter = filter;
                this.autocomputor = autocomputor;
            }

            @Override
            public T get(P param) {
                if (parent.test(filter))
                    return parent.get(param);
                return null;
            }
        }

        public static final class Mapped<P, I, O> extends ParameterizedReference<P, O> {
            private final Function<? super P, ? extends O> action;
            private final Function<? super O, ? extends I> reverse;
            private final Executor autocomputor;

            public Mapped(
                    ParamRef<P, I> parent,
                    Function<? super I, ? extends O> mapper,
                    Function<? super O, ? extends I> reverse,
                    @Nullable Executor autocomputor
            ) {
                this.reverse = reverse;
                this.autocomputor = autocomputor;
                this.action = parent.andThen(mapper);
            }

            @Override
            public O get(P param) {
                return action.apply(param);
            }
        }
    }
}
