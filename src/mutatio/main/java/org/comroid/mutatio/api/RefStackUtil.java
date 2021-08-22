package org.comroid.mutatio.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.*;

public final class RefStackUtil {
    private RefStackUtil() {
        throw new UnsupportedOperationException();
    }

    public abstract static class OutputStack<T> extends RefStack<T> {
        protected OutputStack(
                @Nullable RefStack<?> parent,
                @NotNull String name
        ) {
            super(parent, null, null, Overridability.NONE, name, parent == null ? 0 : parent.index(), null, null);
        }

        @Override
        protected abstract T $get();

        @Override
        protected boolean $set(T newValue) throws IllegalStateException {
            return false; // todo
        }

        @Override
        public boolean isMutable() {
            return false;
        }
    }

    public static <I, O> RefStack<O> $map(RefStack<I> in, final Function<? super I, ? extends O> mapper) {
        return new OutputStack<O>(in, "RefStack.map()") {
            @Override
            protected O $get() {
                return getParent().<RefStack<I>>cast().into(mapper);
            }
        };
    }

    public static <T> RefStack<T> $filter(RefStack<T> in, final Predicate<? super T> tester) {
        return new OutputStack<T>(in, "RefStack.filter()") {
            @Override
            protected T $get() {
                T value = getParent().<RefStack<T>>cast().get();
                if (!tester.test(value))
                    return null;
                return value;
            }

            @Override
            protected boolean $set(T newValue) throws IllegalStateException {
                return getParent().<RefStack<T>>cast().set(newValue);
            }

            @Override
            public boolean isMutable() {
                return getParent().<RefStack<T>>cast().isMutable();
            }
        };
    }

    public static <T> RefStack<T> $or(RefStack<T> in, final Supplier<? extends T> supplier) {
        return new OutputStack<T>(in, "RefStack.or()") {
            @Override
            protected T $get() {
                return getParent().<RefStack<T>>cast().orElseGet(supplier);
            }

            @Override
            protected boolean $set(T newValue) throws IllegalStateException {
                return getParent().<RefStack<T>>cast().set(newValue);
            }

            @Override
            public boolean isMutable() {
                return getParent().<RefStack<T>>cast().isMutable();
            }
        };
    }

    public static <T, A, R> RefStack<R> $combine(RefStack<T> in, final Supplier<A> other, final BiFunction<T, A, R> accumulator) {
        return new OutputStack<R>(null, "RefStack.combine()") {
            @Override
            protected R $get() {
                return getParent().<RefStack<T>>cast().accumulate(other, accumulator);
            }
        };
    }

    public static <T> RefStack<T> $conditional(final BooleanSupplier condition, final Supplier<? extends T> supplier) {
        return new OutputStack<T>(null, "RefStack.conditional()") {
            @Override
            protected T $get() {
                if (condition.getAsBoolean())
                    return supplier.get();
                return null;
            }
        };
    }
}
