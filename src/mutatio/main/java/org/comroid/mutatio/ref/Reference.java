package org.comroid.mutatio.ref;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.CachedValue;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.*;

public interface Reference<T> extends CachedValue<T>, Rewrapper<T> {
    @Override
    @Nullable T get();

    @Override
    Rewrapper<? extends Reference<?>> getParent();

    boolean isMutable();

    default boolean isImmutable() {
        return !isMutable();
    }

    @Deprecated
    default boolean isPresent() {
        return test(Objects::nonNull);
    }

    static <T> Reference<T> constant(@Nullable T of) {
        if (of == null)
            return empty();
        //noinspection unchecked
        return (Reference<T>) Support.IMMUTABLE_CACHE.computeIfAbsent(of, v -> new Support.Default<>(false, of));
    }

    static <T> Reference<T> empty() {
        //noinspection unchecked
        return (Reference<T>) Support.EMPTY;
    }

    static <T> Reference<T> provided(Supplier<T> supplier) {
        return conditional(() -> true, supplier);
    }

    static <T> Reference<T> conditional(BooleanSupplier condition, Supplier<T> supplier) {
        return new Support.Conditional<>(condition, supplier);
    }

    static <T> FutureReference<T> later(CompletableFuture<T> future) {
        return new FutureReference<>(future);
    }

    static <T> Reference<T> create() {
        return create(null);
    }

    static <T> Reference<T> create(@Nullable T initialValue) {
        return create(true, initialValue);
    }

    static <T> Reference<T> create(boolean mutable, @Nullable T initialValue) {
        return new Support.Default<>(mutable, initialValue);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Reference<T> optional(Optional<T> optional) {
        return provided(() -> optional.orElse(null));
    }

    default Processor<T> peek(Consumer<? super T> action) {
        return new Processor.Support.Remapped<>(this, it -> {
            action.accept(it);
            return it;
        }, Function.identity());
    }

    default Processor<T> process() {
        return Processor.ofReference(this);
    }

    default Pipe<T> pipe() {
        return Pipe.of(get());
    }

    /**
     * @return Whether the new value could be set.
     */
    default boolean set(T newValue) {
        return false;
    }

    default boolean unset() {
        return set(null);
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

    void rebind(Supplier<T> behind);

    /**
     * Applies the provided consumer to the current value and attaches a ValueUpdateListener for future updates.
     * @param action The action to apply
     * @return The attached ValueUpdateListener
     */
    default ValueUpdateListener<T> apply(Consumer<T> action) {
        ifPresent(action);
        return onChange(action);
    }

    default Processor<T> filter(Predicate<? super T> predicate) {
        return new Processor.Support.Filtered<>(this, predicate);
    }

    default <R> Processor<R> flatMap(Class<R> type) {
        return filter(type::isInstance).map(type::cast);
    }

    default <R> Processor<R> map(Function<? super T, ? extends R> mapper) {
        return new Processor.Support.Remapped<>(this, mapper, null);
    }

    default <R> Processor<R> flatMap(Function<? super T, ? extends Reference<? extends R>> mapper) {
        return new Processor.Support.ReferenceFlatMapped<>(this, mapper, null);
    }

    default <R> Processor<R> flatMap(Function<? super T, ? extends Reference<? extends R>> mapper, Function<R, T> backwardsConverter) {
        return new Processor.Support.ReferenceFlatMapped<>(this, mapper, backwardsConverter);
    }

    default <R> Processor<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return flatMap(mapper.andThen(opt -> opt.map(Reference::constant).orElseGet(Reference::empty)));
    }

    default <R> Processor<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper, Function<R, T> backwardsConverter) {
        return flatMap(mapper.andThen(Optional::get).andThen(Reference::constant), backwardsConverter);
    }

    @Deprecated
    interface Settable<T> extends Reference<T> {
    }

    @Internal
    final class Support {
        private static final Logger logger = LogManager.getLogger();
        private static final Reference<?> EMPTY = new Default<>(false, null);
        private static final Map<Object, Reference<?>> IMMUTABLE_CACHE = new ConcurrentHashMap<>();

        public static abstract class Base<T> extends CachedValue.Abstract<T> implements Reference<T> {
            protected final AtomicReference<T> atom = new AtomicReference<>();
            private final boolean mutable;
            private Supplier<T> overriddenSupplier = null;

            @Override
            public final Rewrapper<? extends Reference<?>> getParent() {
                return () -> super.getParent().into(Reference.class);
            }

            @Override
            public boolean isMutable() {
                return mutable;
            }

            @Internal
            protected <X> X getFromParent() {
                return getParent().into(ref -> ref.into(Polyfill::uncheckedCast));
            }

            protected Base(boolean mutable) {
                this(null, mutable);
            }

            protected Base(@Nullable CachedValue<?> parent, boolean mutable) {
                super(parent);

                this.mutable = mutable;

                outdate();
            }

            protected abstract T doGet();

            @OverrideOnly
            protected boolean doSet(T value) {
                atom.set(value);
                update(value);
                return true;
            }

            @Nullable
            @Override
            public synchronized final T get() {
                if (isUpToDate() && atom.get() != null) {
                    //logger.trace("{} is up to date; does not need computing", toString());
                    return atom.get();
                }
                return atom.updateAndGet(old -> {
                    //logger.trace("{} is not up to date; recomputing", toString());
                    final T value = overriddenSupplier != null ? overriddenSupplier.get() : doGet();
                    if (value == null)
                        return null;
                    return update(value);
                });
            }

            @Override
            public final boolean set(T value) {
                if (isImmutable())
                    return false;

                boolean doSet = doSet(value);
                if (doSet) {
                    atom.set(value);
                    return update(value) == value;
                }

                return false;
            }

            @Override
            public final void rebind(Supplier<T> behind) {
                if (behind == this || (behind instanceof Processor
                        && ((Processor<T>) behind).upstream().noneMatch(this::equals)))
                    throw new IllegalArgumentException("Cannot rebind behind itself");

                this.overriddenSupplier = behind;
                outdate();
            }

            @Override
            public final String toString() {
                return String.format("Ref#%s<%s; mutable=%s; outdated=%s>",
                        Integer.toHexString(hashCode()), ReflectionHelper.simpleClassName(getClass()), mutable, isOutdated());
            }
        }

        private static class Default<T> extends Base<T> {
            private Default(boolean mutable, T initialValue) {
                super(mutable);

                atom.set(initialValue);
            }

            @Override
            protected T doGet() {
                return atom.get();
            }

            @Override
            protected boolean doSet(T value) {
                atom.set(value);
                outdate();
                return true;
            }
        }

        private static final class Rebound<T> extends Base<T> {
            private final Consumer<T> setter;
            private final Supplier<T> getter;

            @Override
            public boolean isOutdated() {
                return true;
            }

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
                outdate();
                return true;
            }
        }

        private static final class Conditional<T> extends Base<T> {
            private final BooleanSupplier condition;
            private final Supplier<T> supplier;

            @Override
            public boolean isOutdated() {
                return true;
            }

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
