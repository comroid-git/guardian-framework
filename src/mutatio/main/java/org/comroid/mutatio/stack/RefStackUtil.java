package org.comroid.mutatio.stack;

import java.util.concurrent.CompletableFuture;
import java.util.function.*;

public final class RefStackUtil {
    private RefStackUtil() {
        throw new UnsupportedOperationException();
    }

    public static <T> RefStack<T> $future(CompletableFuture<T> future) {
        return new OutputStack<T>(null, "RefStack.future()") {
            @Override
            protected T $get() {
                return future.join();
            }
        };
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
        return new MutableStack<T>(in, "RefStack.filter()") {
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
        return new MutableStack<T>(in, "RefStack.or()") {
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
