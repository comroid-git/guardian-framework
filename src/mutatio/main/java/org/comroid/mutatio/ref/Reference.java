package org.comroid.mutatio.ref;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.model.ReferenceOverwriter;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.*;

public abstract class Reference<T> extends ValueProvider.NoParam<T> implements Ref<T> {
    private final boolean mutable;
    private Predicate<T> overriddenSetter;

    @Internal
    protected <X> X getFromParent() {
        return getParent().into(ref -> ref.into(Polyfill::uncheckedCast));
    }

    @Override
    public boolean isMutable() {
        return mutable;
    }

    protected Reference(
            boolean mutable
    ) {
        this(null, mutable, null);
    }

    protected Reference(
            @Nullable Reference<?> parent
    ) {
        this(parent, false, parent != null ? parent.getAutocomputor() : null);
    }

    protected Reference(
            @Nullable ValueProvider.NoParam<?> parent,
            boolean mutable
    ) {
        this(parent, mutable, parent != null ? parent.getAutocomputor() : null);
    }

    protected Reference(
            @Nullable ValueProvider.NoParam<?> parent,
            boolean mutable,
            Executor autoComputor
    ) {
        this(parent, null, mutable, autoComputor);
    }

    protected <X> Reference(
            final @Nullable Reference<X> parent,
            final @NotNull Function<T, X> backwardsConverter
    ) {
        this(parent, backwardsConverter, parent != null ? parent.getAutocomputor() : null);
    }

    protected <X> Reference(
            final @Nullable Reference<X> parent,
            final @NotNull Function<T, X> backwardsConverter,
            Executor autoComputor
    ) {
        this(parent, t -> parent != null && parent.set(backwardsConverter.apply(t)), parent != null, autoComputor);
    }

    private Reference(
            @Nullable ValueProvider.NoParam<?> parent,
            @Nullable Predicate<T> setter,
            boolean mutable,
            Executor autoComputor
    ) {
        super(parent, autoComputor);

        this.overriddenSetter = setter;
        this.mutable = mutable;
    }

    public static <T> Reference<T> constant(@Nullable T of) {
        if (of == null)
            return empty();
        //noinspection unchecked
        return (Reference<T>) Support.IMMUTABLE_CACHE.computeIfAbsent(of, v -> new Support.Default<>(false, of));
    }

    public static <T> Reference<T> empty() {
        //noinspection unchecked
        return (Reference<T>) Support.EMPTY;
    }

    public static <T> Reference<T> provided(Supplier<T> supplier) {
        return conditional(() -> true, supplier);
    }

    public static <T> Reference<T> conditional(BooleanSupplier condition, Supplier<T> supplier) {
        return new Support.Conditional<>(condition, supplier);
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
        return new Support.Default<>(mutable, initialValue);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static <T> Reference<T> optional(Optional<T> optional) {
        return provided(() -> optional.orElse(null));
    }

    @Override
    public final @Nullable T get() {
        return super.get(null);
    }

    @OverrideOnly
    protected boolean doSet(T value) {
        putIntoCache(value);
        return true;
    }

    @Override
    public Reference<T> peek(Consumer<? super T> action) {
        return new Reference.Support.Remapped<>(this, it -> {
            action.accept(it);
            return it;
        }, Function.identity());
    }

    @Override
    public boolean unset() {
        return set(null);
    }

    @Override
    public <X, R> Reference<R> combine(final Supplier<X> other, final BiFunction<T, X, R> accumulator) {
        return new Reference.Support.Remapped<>(this, it -> accumulator.apply(it, other.get()), null);
    }

    @Override
    public final <P, R> ParameterizedReference<P, R> addParameter(BiFunction<T, P, R> source) {
        return new ParameterizedReference.Support.Source<>(this, source);
    }

    @Override
    public final boolean set(T value) {
        if (isImmutable())
            return false;

        boolean doSet = overriddenSetter == null ? doSet(value) : overriddenSetter.test(value);
        if (!doSet)
            return false;
        overriddenSupplier = null;
        putIntoCache(value);
        return true;
    }

    @Override
    public final void rebind(final Supplier<T> behind) {
        if (behind == this || (behind instanceof Reference && dependsOn((Reference<T>) behind)))
            throw new IllegalArgumentException("Circular Reference Dependency detected");

        overriddenSupplier = nil -> behind.get();
        if (behind instanceof Reference)
            overriddenSetter = ((Reference<T>) behind)::set;
        outdateCache();
    }

    @Override
    public Reference<T> filter(Predicate<? super T> predicate) {
        return new Reference.Support.Filtered<>(this, predicate);
    }

    @Override
    public <R> Reference<R> map(Function<? super T, ? extends R> mapper) {
        return map(mapper, null);
    }

    @Override
    public <R> Reference<R> map(Function<? super T, ? extends R> mapper, Function<R, T> backwardsConverter) {
        return new Reference.Support.Remapped<>(this, mapper, backwardsConverter);
    }

    @Override
    public <R> Reference<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return flatMap(mapper, null);
    }

    @Override
    public <R> Reference<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper, Function<R, T> backwardsConverter) {
        return new Reference.Support.ReferenceFlatMapped<>(this, mapper, backwardsConverter);
    }

    @Override
    public Reference<T> or(Supplier<? extends T> orElse) {
        return new Support.Or<>(this, orElse);
    }

    @Override
    public final String toString() {
        return ifPresentMapOrElseGet(String::valueOf, () -> "null");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Reference && (contentEquals(((Reference<?>) other).get()) || other == this);
    }

    public interface Advancer<I, O> extends ReferenceOverwriter<I, O, Reference<I>, Reference<O>> {
        Reference<O> advance(Reference<I> ref);
    }

    @Internal
    public static final class Support {
        private static final Logger logger = LogManager.getLogger();
        private static final Reference<?> EMPTY = new Default<>(false, null);
        private static final Map<Object, Reference<?>> IMMUTABLE_CACHE = new ConcurrentHashMap<>();

        @Deprecated
        public static abstract class Base<T> extends Reference<T> {
            @Deprecated
            protected Base(boolean mutable) {
                this(null, mutable);
            }

            @Deprecated
            protected Base(@Nullable ValueProvider.NoParam<?> parent, boolean mutable) {
                super(parent, mutable);
            }
        }

        private static class Default<T> extends Reference<T> {
            private Default(boolean mutable, T initialValue) {
                super(mutable);

                putIntoCache(initialValue);
            }

            @Override
            protected T doGet() {
                return getFromCache();
            }

            @Override
            protected boolean doSet(T value) {
                putIntoCache(value);
                return true;
            }
        }

        private static final class Rebound<T> extends Reference<T> {
            private final Consumer<T> setter;
            private final Supplier<T> getter;

            /*
                        @Override
                        public boolean isOutdated() {
                            return true;
                        }
             */
            public Rebound(Consumer<T> setter, Supplier<T> getter) {
                super(true);

                this.setter = setter;
                this.getter = getter;
            }

            @Override
            protected T doGet() {
                return getter.get();
            }

            @Override
            protected boolean doSet(T value) {
                setter.accept(value);
                outdateCache();
                return true;
            }
        }

        private static final class Conditional<T> extends Reference<T> {
            private final BooleanSupplier condition;
            private final Supplier<T> supplier;

            /*
                        @Override
                        public boolean isOutdated() {
                            return true;
                        }
            */
            public Conditional(BooleanSupplier condition, Supplier<T> supplier) {
                super(false);

                this.condition = condition;
                this.supplier = supplier;
            }

            @Override
            protected T doGet() {
                if (condition.getAsBoolean())
                    return supplier.get();
                return null;
            }
        }

        public static class Identity<T> extends Reference<T> {
            public Identity(Reference<T> parent) {
                super(parent);
            }

            @Override
            protected T doGet() {
                return getFromParent();
            }
        }

        public static final class Filtered<T> extends Reference<T> {
            private final Predicate<? super T> filter;

            public Filtered(Reference<T> base, Predicate<? super T> filter) {
                super(base, Function.identity());

                this.filter = filter;
            }

            @Override
            protected T doGet() {
                final T value = getFromParent();

                if (value != null && filter.test(value))
                    return value;
                return null;
            }
        }

        public static final class Remapped<I, O> extends Reference<O> {
            private final Function<? super I, ? extends O> remapper;

            public <R> Remapped(
                    Reference<I> base,
                    Function<? super I, ? extends O> remapper,
                    Function<O, I> backwardsConverter
            ) {
                super(base, backwardsConverter);

                this.remapper = remapper;
            }

            @Override
            protected O doGet() {
                final I in = getFromParent();

                if (in != null)
                    return remapper.apply(in);
                return null;
            }
        }

        public static final class ReferenceFlatMapped<I, O> extends Reference<O> {
            private final Function<? super I, ? extends Rewrapper<? extends O>> remapper;

            public ReferenceFlatMapped(
                    Reference<I> base,
                    Function<? super I, ? extends Rewrapper<? extends O>> remapper,
                    Function<O, I> backwardsConverter
            ) {
                super(base, backwardsConverter);

                this.remapper = remapper;
            }

            @Override
            protected O doGet() {
                final I in = getFromParent();

                if (in != null)
                    return remapper.apply(in).orElse(null);
                return null;
            }
        }

        public static final class Or<T> extends Reference<T> {
            private final Supplier<? extends T> other;

            public Or(Reference<T> base, Supplier<? extends T> other) {
                super(base, Function.identity());

                this.other = other;
            }

            @Override
            protected T doGet() {
                final T in = getFromParent();

                if (in == null)
                    return other.get();
                return in;
            }
        }
    }
}
