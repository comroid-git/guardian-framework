package org.comroid.mutatio.adapter;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class StageAdapter<In, Out>
        extends ReferenceStageAdapter<@Nullable("always") Void, @Nullable("always") Void, In, Out, Reference<In>, Reference<Out>>
        implements Reference.Advancer<In, Out> {
    protected StageAdapter(boolean isIdentity, final Function<? super In, ? extends Out> valueMapper) {
        super(isIdentity, Function.identity(), (nil, in) -> valueMapper.apply(in));
    }

    static <T> StageAdapter<T, T> filter(Predicate<? super T> predicate) {
        return new Filter<>(predicate);
    }

    static <O, T> StageAdapter<O, T> map(Function<? super O, ? extends T> mapper) {
        return new Map<>(mapper);
    }

    static <O, T> StageAdapter<O, T> flatMap(Function<? super O, ? extends Rewrapper<? extends T>> mapper) {
        return map(mapper.andThen(Rewrapper::get));
    }

    @Deprecated // todo: fix
    static <T> StageAdapter<T, T> distinct() {
        return filter(new HashSet<>()::add);
    }

    static <T> StageAdapter<T, T> peek(Consumer<? super T> action) {
        return filter(new Structure.ConsumingFilter<>(action));
    }

    @Deprecated // todo: fix
    static <T> StageAdapter<T, T> limit(long limit) {
        return filter(new Structure.Limiter<>(limit));
    }

    @Deprecated // todo: fix
    static <T> StageAdapter<T, T> skip(long skip) {
        return filter(new Structure.Skipper<>(skip));
    }

    static <In, T> StageAdapter<In, T> identity() {
        return map(Polyfill::uncheckedCast);
    }

    public final Out advanceValue(In value) {
        return advanceValue(null, value);
    }

    @OverrideOnly
    @Deprecated
    public Out convertValue(In value) {
        return advanceValue(null, value);
    }

    private static final class Filter<T> extends StageAdapter<T, T> {
        private final Predicate<? super T> predicate;

        public Filter(Predicate<? super T> predicate) {
            super(true, Function.identity());

            this.predicate = predicate;
        }

        @Override
        public Reference<T> advance(Reference<T> ref) {
            return new Reference.Support.Filtered<>(ref, predicate);
        }
    }

    private static final class Map<O, T> extends StageAdapter<O, T> {
        public Map(Function<? super O, ? extends T> mapper) {
            super(false, mapper);
        }

        @Override
        public Reference<T> advance(Reference<O> ref) {
            return new Reference.Support.Remapped<>(ref, this::advanceValue, null);
        }
    }
}
