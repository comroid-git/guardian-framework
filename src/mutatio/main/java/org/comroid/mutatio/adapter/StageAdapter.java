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

    @Override
    public final @Nullable("always") Void advanceKey(@Nullable("always") Void key) {
        return key;
    }

    @OverrideOnly
    @Deprecated
    public Out convertValue(In value) {
        return advanceValue(null, value);
    }

    private static final class Filter<T> extends StageAdapter<T, T> {
        private final Predicate<? super T> predicate;

        @Override
        public boolean isIdentityValue() {
            return true;
        }

        public Filter(Predicate<? super T> predicate) {
            this.predicate = predicate;
        }

        @Override
        public Reference<T> advance(Reference<T> ref) {
            return new Reference.Support.Filtered<>(ref, predicate);
        }

        @Override
        public T advanceValue(Void nil, T value) {
            return value;
        }
    }

    private static final class Map<O, T> extends StageAdapter<O, T> {
        private final Function<? super O, ? extends T> mapper;

        @Override
        public boolean isIdentityValue() {
            return false;
        }

        public Map(Function<? super O, ? extends T> mapper) {
            this.mapper = mapper;
        }

        @Override
        public Reference<T> advance(Reference<O> ref) {
            return new Reference.Support.Remapped<>(ref, mapper, null);
        }

        @Override
        public T advanceValue(Void nil, O value) {
            return mapper.apply(value);
        }
    }
}
