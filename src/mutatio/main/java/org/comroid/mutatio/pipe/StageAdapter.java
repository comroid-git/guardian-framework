package org.comroid.mutatio.pipe;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.Processor;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface StageAdapter<In, Out, RI extends Reference<In>, RO extends Reference<Out>> {
    static <T> StageAdapter<T, T, Reference<T>, Reference<T>> filter(Predicate<? super T> predicate) {
        return new Support.Filter<>(predicate);
    }

    static <O, T> StageAdapter<O, T, Reference<O>, Reference<T>> map(Function<? super O, ? extends T> mapper) {
        return new Support.Map<>(mapper);
    }

    static <O, T> StageAdapter<O, T, Reference<O>, Reference<T>> flatMap(Function<? super O, ? extends Rewrapper<? extends T>> mapper) {
        return new Support.Map<>(mapper.andThen(Rewrapper::get));
    }

    static <T> StageAdapter<T, T, Reference<T>, Reference<T>> distinct() {
        return filter(new HashSet<>()::add);
    }

    static <T> StageAdapter<T, T, Reference<T>, Reference<T>> peek(Consumer<? super T> action) {
        return filter(new Structure.ConsumingFilter<>(action));
    }

    static <T> StageAdapter<T, T, Reference<T>, Reference<T>> limit(long limit) {
        return filter(new Structure.Limiter<>(limit));
    }

    static <T> StageAdapter<T, T, Reference<T>, Reference<T>> skip(long skip) {
        return filter(new Structure.Skipper<>(skip));
    }

    RO advance(RI ref);

    @OverrideOnly
    default Out convertValue(In value) {
        return Polyfill.uncheckedCast(value);
    }

    final class Structure {
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

            protected Skipper(long skip) {
                this.skip = skip;
            }

            @Override
            public boolean test(T t) {
                return c++ >= skip;
            }
        }
    }

    final class Support {
        public static final class Filter<T> implements StageAdapter<T, T, Reference<T>, Reference<T>> {
            private final Predicate<? super T> predicate;

            public Filter(Predicate<? super T> predicate) {
                this.predicate = predicate;
            }

            @Override
            public Reference<T> advance(Reference<T> ref) {
                return new Processor.Support.Filtered<>(ref, predicate);
            }
        }

        public static final class Map<O, T> implements StageAdapter<O, T, Reference<O>, Reference<T>> {
            private final Function<? super O, ? extends T> mapper;

            public Map(Function<? super O, ? extends T> mapper) {
                this.mapper = mapper;
            }

            @Override
            public Reference<T> advance(Reference<O> ref) {
                return new Processor.Support.Remapped<>(ref, mapper, null);
            }

            @Override
            public T convertValue(O value) {
                return mapper.apply(value);
            }
        }
    }
}
