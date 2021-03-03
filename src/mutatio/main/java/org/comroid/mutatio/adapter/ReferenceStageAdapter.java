package org.comroid.mutatio.adapter;

import org.comroid.mutatio.pipe.ReferenceConverter;
import org.comroid.mutatio.ref.Reference;

import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class ReferenceStageAdapter<InK, OutK, InV, OutV, InRef extends Reference<InV>, OutRef extends Reference<OutV>>
        implements ReferenceConverter<InRef, OutRef> {
    @Override
    public abstract OutRef advance(InRef reference);

    public abstract OutK advanceKey(InK key);

    public abstract OutV advanceValue(InK key, InV value);

    public static final class Structure {
        public static final class ConsumingFilter<T> implements Predicate<T> {
            private final Consumer<? super T> action;

            protected ConsumingFilter(Consumer<? super T> action) {
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
            protected Limiter(long limit) {
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
            protected Skipper(long skip) {
                this.skip = skip;
            }

            @Override
            public boolean test(T t) {
                return c++ >= skip;
            }
        }
    }
}
