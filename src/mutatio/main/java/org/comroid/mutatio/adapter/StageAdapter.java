package org.comroid.mutatio.adapter;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.model.Structure;
import org.comroid.mutatio.ref.KeyedReference;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@ApiStatus.Experimental // todo: fix
public abstract class StageAdapter<In, Out>
        extends ReferenceStageAdapter<@Nullable Integer, @NotNull Integer, In, Out, KeyedReference<@NotNull Integer, In>, KeyedReference<@NotNull Integer, Out>>
        implements KeyedReference.Advancer<@NotNull Integer, In, @NotNull Integer, Out> {
    protected StageAdapter(boolean isIdentity, final Function<? super In, ? extends Out> valueMapper) {
        super(isIdentity, Function.identity(), (nil, in) -> valueMapper.apply(in));
    }

    public static <T> StageAdapter<T, T> filter(Predicate<? super T> predicate) {
        return new Filter<>(predicate);
    }

    public static <O, T> StageAdapter<O, T> map(Function<? super O, ? extends T> mapper) {
        return new Map<>(mapper);
    }

    public static <O, T> StageAdapter<O, T> flatMap(Function<? super O, ? extends Rewrapper<? extends T>> mapper) {
        return map(mapper.andThen(Rewrapper::get));
    }

    @Deprecated // todo: fix
    public static <T> StageAdapter<T, T> distinct() {
        return filter(new HashSet<>()::add);
    }

    public static <T> StageAdapter<T, T> peek(Consumer<? super T> action) {
        return filter(new Structure.PeekAction<>(action));
    }

    @Deprecated // todo: fix
    public static <T> StageAdapter<T, T> limit(long limit) {
        return filter(new Structure.Limiter<>(limit));
    }

    @Deprecated // todo: fix
    public static <T> StageAdapter<T, T> skip(long skip) {
        return filter(new Structure.Skipper<>(skip));
    }

    public static <In, T> StageAdapter<In, T> identity() {
        return map(Polyfill::uncheckedCast);
    }

    public final Out advanceValue(In value) {
        return advanceValue(null, value);
    }

    @Override
    public final String toString() {
        return String.format("StageAdapter(%s)@%d", getClass().getSimpleName(), hashCode());
    }

    private static final class Filter<T> extends StageAdapter<T, T> {
        private final Predicate<? super T> predicate;

        public Filter(Predicate<? super T> predicate) {
            super(true, Function.identity());

            this.predicate = predicate;
        }

        @Override
        public KeyedReference<@NotNull Integer, T> advance(KeyedReference<@NotNull Integer, T> reference) {
            return new KeyedReference.Support.Filtered<>(reference, (i, v) -> predicate.test(v));
        }
    }

    private static final class Map<O, T> extends StageAdapter<O, T> {
        public Map(Function<? super O, ? extends T> mapper) {
            super(false, mapper);
        }

        @Override
        public KeyedReference<@NotNull Integer, T> advance(KeyedReference<@NotNull Integer, O> reference) {
            return new KeyedReference.Support.Mapped<>(reference, this::advanceKey, this::advanceValue);
        }
    }
}
