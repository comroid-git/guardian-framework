package org.comroid.mutatio.ref;

import org.comroid.api.Polyfill;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Cloneable through {@link #process()}.
 */
public interface Processor<T> extends Reference<T>, Cloneable, AutoCloseable {
    static <T> Processor<T> ofReference(Reference<T> reference) {
        return new Support.Identity<>(reference);
    }

    static <T> Processor<T> empty() {
        //noinspection unchecked
        return (Processor<T>) Support.EMPTY;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    static <T> Processor<T> ofOptional(Optional<T> optional) {
        return optional.map(Processor::ofConstant)
                .orElseGet(Processor::empty);
    }

    static <T> Processor<T> ofConstant(T value) {
        return ofReference(Objects.isNull(value) ? Reference.empty() : Reference.constant(value));
    }

    static <T> Processor<T> providedOptional(Supplier<Optional<T>> supplier) {
        return new Support.Identity<>(Reference.provided(() -> supplier.get().orElse(null)));
    }

    default Stream<Reference<?>> upstream() {
        return Stream.concat(
                Stream.of(this),
                getParent().ifPresentMapOrElseGet(
                        ref -> {
                            if (ref instanceof Processor)
                                return ((Processor<?>) ref).upstream();
                            else return Stream.of(ref);
                        }, Stream::empty));
    }

    /**
     * Consumes the processor and terminates.
     * Used for completing some calls that return {@linkplain Processor processors}.
     */
    @Override
    default void close() {
        get();
    }

    @Override
    default Processor<T> filter(Predicate<? super T> predicate) {
        return new Support.Filtered<>(this, predicate);
    }

    @Override
    default <R> Processor<R> map(Function<? super T, ? extends R> mapper) {
        return new Support.Remapped<>(this, mapper, null);
    }

    @Override
    default Processor<T> process() {
        return this;
    }

    @Override
    default <R> Processor<R> flatMap(Function<? super T, ? extends Reference<? extends R>> mapper) {
        return flatMap(mapper, null);
    }

    @Override
    default <R> Processor<R> flatMapOptional(Function<? super T, ? extends Optional<? extends R>> mapper) {
        return flatMapOptional(mapper, null);
    }

    default Processor<T> or(final Supplier<T> other) {
        return new Support.Or<>(this, other);
    }

    @FunctionalInterface
    interface Advancer<I, O> {
        Processor<O> advance(Processor<I> ref);
    }

    @Internal
    final class Support {
        public static final Processor<?> EMPTY = new Identity<>(Reference.empty());

        public static abstract class Base<I, O> extends Reference.Support.Base<O> implements Processor<O> {
            private final Predicate<O> setter;

            @Override
            public boolean isMutable() {
                return getParent().test(Reference::isMutable) || setter != null;
            }

            protected Base(Reference<I> parent) {
                this(parent, (Predicate<O>) null);
            }

            protected Base(Reference<I> parent, Function<O, I> backwardsConverter) {
                this(
                        parent,
                        (backwardsConverter != null && parent.isMutable())
                                ? ((Predicate<O>) it -> parent.set(backwardsConverter.apply(it)))
                                : null
                );
            }

            protected Base(Reference<I> parent, Predicate<O> setter) {
                super(Objects.requireNonNull(parent, "Parent missing"), setter != null);

                this.setter = setter;
            }

            @Override
            protected boolean doSet(O value) {
                assert setter != null : "isMutable check wrong";
                return setter.test(value);
            }
        }

        public static class Identity<T> extends Base<T, T> {
            public Identity(Reference<T> parent) {
                super(parent);
            }

            @Override
            protected T doGet() {
                return getFromParent();
            }
        }

        public static final class Filtered<T> extends Base<T, T> {
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

        public static final class Remapped<I, O> extends Base<I, O> {
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

        public static final class ReferenceFlatMapped<I, O> extends Base<I, O> {
            private final Function<? super I, ? extends Reference<? extends O>> remapper;

            public ReferenceFlatMapped(
                    Reference<I> base,
                    Function<? super I, ? extends Reference<? extends O>> remapper,
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

        public static final class Or<T> extends Base<T, T> {
            private final Supplier<T> other;

            @Override
            public boolean isOutdated() {
                return true;
            }

            public Or(Reference<T> base, Supplier<T> other) {
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
