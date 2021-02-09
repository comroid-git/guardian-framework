package org.comroid.mutatio.ref;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.SingleValueCache;
import org.comroid.mutatio.cache.ValueUpdateListener;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.util.ReflectionHelper;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

public abstract class Reference<T> extends SingleValueCache.Abstract<T> implements SingleValueCache<T>, Rewrapper<T> {
    private final boolean mutable;
    private Supplier<T> overriddenSupplier = null;

    @Internal
    protected <X> X getFromParent() {
        return getParent().into(ref -> ref.into(Polyfill::uncheckedCast));
    }

    @Override
    public final Rewrapper<? extends Reference<?>> getParent() {
        return () -> super.getParent().into(Reference.class);
    }

    public boolean isMutable() {
        return mutable;
    }

    public boolean isImmutable() {
        return !isMutable();
    }

    @Deprecated
    public boolean isPresent() {
        return test(Objects::nonNull);
    }

    protected Reference(boolean mutable) {
        this(null, mutable);
    }

    protected Reference(@Nullable SingleValueCache<?> parent, boolean mutable) {
        super(parent);

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

    protected abstract T doGet();

    @OverrideOnly
    protected boolean doSet(T value) {
        putIntoCache(value);
        return true;
    }

    @Override
    public synchronized final T get() {
        T fromCache = getFromCache();
        if (isUpToDate() && fromCache != null) {
            //logger.trace("{} is up to date; does not need computing", toString());
            return fromCache;
        }
        //logger.trace("{} is not up to date; recomputing", toString());
        T value = overriddenSupplier != null ? overriddenSupplier.get() : doGet();
        if (value == null)
            return null;
        return putIntoCache(value);
    }

    public Processor<T> peek(Consumer<? super T> action) {
        return new Processor.Support.Remapped<>(this, it -> {
            action.accept(it);
            return it;
        }, Function.identity());
    }

    public Processor<T> process() {
        return Processor.ofReference(this);
    }

    public Pipe<T> pipe() {
        return Pipe.of(get());
    }

    public boolean unset() {
        return set(null);
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    public T compute(Function<T, T> computor) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        if (!set(into(computor)))
            throw new UnsupportedOperationException("Could not set value");
        return get();
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    public T computeIfPresent(Function<T, T> computor) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        if (!isNull() && !set(into(computor)))
            throw new UnsupportedOperationException("Could not set value");
        return get();
    }

    /**
     * @return The new value if it could be set, else the previous value.
     */
    public T computeIfAbsent(Supplier<T> supplier) {
        if (isImmutable())
            throw new UnsupportedOperationException("Reference is immutable");
        if (isNull() && !set(supplier.get()))
            throw new UnsupportedOperationException("Could not set value");
        return get();
    }

    @Override
    public <X, R> Reference<R> combine(final Supplier<X> other, final BiFunction<T, X, R> accumulator) {
        return new Processor.Support.Remapped<>(this, it -> accumulator.apply(it, other.get()), null);
    }

    public final boolean set(T value) {
        if (isImmutable())
            return false;

        boolean doSet = doSet(value);
        if (!doSet)
            return false;
        putIntoCache(value);
        return true;
    }

    public final void rebind(Supplier<T> behind) {
        if (behind == this || (behind instanceof Processor
                && ((Processor<T>) behind).upstream().noneMatch(this::equals)))
            throw new IllegalArgumentException("Cannot rebind behind itself");

        this.overriddenSupplier = behind;
        outdateCache();
    }

    /**
     * Applies the provided consumer to the current value and attaches a ValueUpdateListener for future updates.
     *
     * @param action The action to apply
     * @return The attached ValueUpdateListener
     */
    public ValueUpdateListener<T> apply(Consumer<T> action) {
        ifPresent(action);
        return onChange(action);
    }

    public Processor<T> filter(Predicate<? super T> predicate) {
        return new Processor.Support.Filtered<>(this, predicate);
    }

    public <R> Processor<R> flatMap(Class<R> type) {
        return filter(type::isInstance).map(type::cast);
    }

    public <R> Processor<R> map(Function<? super T, ? extends R> mapper) {
        return new Processor.Support.Remapped<>(this, mapper, null);
    }

    public <R> Processor<R> flatMap(Function<? super T, ? extends Reference<? extends R>> mapper) {
        return new Processor.Support.ReferenceFlatMapped<>(this, mapper, null);
    }

    public <R> Processor<R> flatMap(Function<? super T, ? extends Reference<? extends R>> mapper, Function<R, T> backwardsConverter) {
        return new Processor.Support.ReferenceFlatMapped<>(this, mapper, backwardsConverter);
    }

    public <R> Processor<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return flatMap(mapper.andThen(opt -> opt.map(Reference::constant).orElseGet(Reference::empty)));
    }

    public <R> Processor<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper, Function<R, T> backwardsConverter) {
        return flatMap(mapper.andThen(Optional::get).andThen(Reference::constant), backwardsConverter);
    }

    @Override
    public final String toString() {
        return String.format("Ref#%s<%s; mutable=%s; outdated=%s>",
                Integer.toHexString(hashCode()), ReflectionHelper.simpleClassName(getClass()), mutable, isOutdated());
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Reference && (contentEquals(((Reference<?>) other).get()) || other == this);
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
            protected Base(@Nullable SingleValueCache<?> parent, boolean mutable) {
                super(parent, mutable);
            }
        }

        private static class Default<T> extends Base<T> {
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

        private static final class Rebound<T> extends Base<T> {
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

        private static final class Conditional<T> extends Base<T> {
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
    }
}
