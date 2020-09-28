package org.comroid.mutatio.pipe;

import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.ref.Reference;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface StageAdapter<O, T> {
    static <T> StageAdapter<T, T> filter(Predicate<? super T> predicate) {
        return new Support.Filter<>(predicate);
    }

    static <O, T> StageAdapter<O, T> map(Function<? super O, ? extends T> mapper) {
        return new Support.Map<>(mapper);
    }

    static <O, T> StageAdapter<O, T> flatMap(Function<? super O, ? extends Reference<? extends T>> mapper) {
        return new Support.Map<>(mapper.andThen(Reference::get));
    }

    static <T> StageAdapter<T, T> distinct() {
        class DistinctionPredicate implements Predicate<T> {
            private final Set<T> set = new HashSet<>();

            @Override
            public boolean test(T it) {
                return set.add(it);
            }
        }

        return filter(new DistinctionPredicate());
    }

    static <T> StageAdapter<T, T> peek(Consumer<? super T> action) {
        class ConsumingFilter implements Predicate<T> {
            private final Consumer<? super T> action;

            private ConsumingFilter(Consumer<? super T> action) {
                this.action = action;
            }

            @Override
            public boolean test(T it) {
                action.accept(it);

                return true;
            }
        }

        return filter(new ConsumingFilter(action));
    }

    static <T> StageAdapter<T, T> limit(long limit) {
        class Limiter implements Predicate<T> {
            private final long limit;
            private long c = 0;

            private Limiter(long limit) {
                this.limit = limit;
            }

            @Override
            public boolean test(T t) {
                return c++ < limit;
            }
        }

        return filter(new Limiter(limit));
    }

    static <T> StageAdapter<T, T> skip(long skip) {
        class Skipper implements Predicate<T> {
            private final long skip;
            private long c = 0;

            private Skipper(long skip) {
                this.skip = skip;
            }

            @Override
            public boolean test(T t) {
                return c++ >= skip;
            }
        }

        return filter(new Skipper(skip));
    }

    Reference<T> advance(Reference<O> ref);

    final class Support {
        private static final class Filter<T> implements StageAdapter<T, T> {
            private final Predicate<? super T> predicate;

            public Filter(Predicate<? super T> predicate) {
                this.predicate = predicate;
            }

            @Override
            public Reference<T> advance(Reference<T> ref) {
                return new Processor.Support.Filtered<>(ref, predicate);
            }
        }

        private static final class Map<O, T> implements StageAdapter<O, T> {
            private final Function<? super O, ? extends T> mapper;

            public Map(Function<? super O, ? extends T> mapper) {
                this.mapper = mapper;
            }

            @Override
            public Reference<T> advance(Reference<O> ref) {
                return new Processor.Support.Remapped<>(ref, mapper, null);
            }
        }
    }
}
