package org.comroid.common.exception;

import org.comroid.api.ThrowingRunnable;
import org.comroid.api.ThrowingSupplier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public final class ThrowableForwarder<T extends Throwable, O extends RuntimeException> {
    private final Function<T, O> acceptor;

    public ThrowableForwarder(Function<T, O> acceptor) {
        this.acceptor = acceptor;
    }

    public static <T extends Throwable> ThrowableForwarder<T, RuntimeException> rethrowing() {
        return new ThrowableForwarder<>(RuntimeException::new);
    }

    public void handle(T throwable) {
        final O result = acceptor.apply(throwable);

        if (result != null)
            throw result;
    }

    public <R> @NotNull R request(ThrowingSupplier<? extends R, T> supplier) throws O {
        try {
            return supplier.get();
        } catch (Throwable throwable) {
            //noinspection unchecked
            handle((T) throwable);
        }

        throw new AssertionException("Could not handle throwable");
    }

    public void execute(ThrowingRunnable<T> supplier) throws O {
        try {
            supplier.run();
        } catch (Throwable throwable) {
            //noinspection unchecked
            handle((T) throwable);
        }
    }
}
