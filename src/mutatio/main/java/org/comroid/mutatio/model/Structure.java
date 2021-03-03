package org.comroid.mutatio.model;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class Structure {
    private Structure() {
        throw new UnsupportedOperationException();
    }

    public static final class YieldAction<T> implements Predicate<T> {
        private final Predicate<? super T> predicate;
        private final Consumer<? super T> elseConsume;

        public YieldAction(Predicate<? super T> predicate, Consumer<? super T> elseConsume) {
            this.predicate = predicate;
            this.elseConsume = elseConsume;
        }

        @Override
        public boolean test(T t) {
            if (predicate.test(t))
                return true;
            elseConsume.accept(t);
            return false;
        }
    }

    public static final class PeekAction<T> implements Predicate<T> {
        private final Consumer<? super T> action;

        public PeekAction(Consumer<? super T> action) {
            this.action = action;
        }

        @Override
        public boolean test(T it) {
            action.accept(it);

            return true;
        }
    }

    public static final class Limiter<T> implements Predicate<T> {
        private final long limit;
        private long c = 0;

        @Deprecated // todo: fix
        public Limiter(long limit) {
            this.limit = limit;
        }

        @Override
        public boolean test(T t) {
            return c++ < limit;
        }
    }

    public static final class Skipper<T> implements Predicate<T> {
        private final long skip;
        private long c = 0;

        @Deprecated // todo: fix
        public Skipper(long skip) {
            this.skip = skip;
        }

        @Override
        public boolean test(T t) {
            return c++ >= skip;
        }
    }

    public static final class DistinctFilter<T> implements Predicate<T> {
        private final Set<T> items = new HashSet<>();

        @Override
        public boolean test(T t) {
            return items.add(t);
        }
    }
}
