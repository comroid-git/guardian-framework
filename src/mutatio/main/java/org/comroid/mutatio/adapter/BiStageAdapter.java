package org.comroid.mutatio.adapter;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.function.*;

public abstract class BiStageAdapter<InK, InV, OutK, OutV>
        extends ReferenceStageAdapter<InK, OutK, InV, OutV, KeyedReference<InK, InV>, KeyedReference<OutK, OutV>>
        implements KeyedReference.Advancer<InK, InV, OutK, OutV> {
    protected BiStageAdapter(
            boolean isIdentity,
            Function<? super InK, ? extends OutK> keyMapper,
            final Function<? super InV, ? extends OutV> valueMapper
    ) {
        this(isIdentity, keyMapper, (unused, in) -> valueMapper.apply(in));
    }

    protected BiStageAdapter(
            boolean isIdentity,
            Function<? super InK, ? extends OutK> keyMapper,
            BiFunction<? super InK, ? super InV, ? extends OutV> valueMapper
    ) {
        super(isIdentity, keyMapper, valueMapper);
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterKey(final Predicate<? super K> predicate) {
        return filterBoth((k, unused) -> predicate.test(k));
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterValue(final Predicate<? super V> predicate) {
        return filterBoth((unused, v) -> predicate.test(v));
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterBoth(final BiPredicate<? super K, ? super V> predicate) {
        return new Filter<>(predicate);
    }

    static <K, V, R> BiStageAdapter<K, V, R, V> mapKey(Function<? super K, ? extends R> mapper) {
        return new Map<>(mapper, Function.identity());
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> mapValue(Function<? super V, ? extends R> mapper) {
        return new Map<>(Function.identity(), mapper);
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> mapBoth(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return new Map<>(Function.identity(), mapper);
    }

    static <K, V, R> BiStageAdapter<K, V, R, V> flatMapKey(Function<? super K, ? extends Rewrapper<? extends R>> mapper) {
        return new Map<>(mapper.andThen(Rewrapper::get), Function.identity());
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> flatMapValue(Function<? super V, ? extends Rewrapper<? extends R>> mapper) {
        return new Map<>(Function.identity(), mapper.andThen(Rewrapper::get));
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> flatMapBoth(BiFunction<? super K, ? super V, ? extends Rewrapper<? extends R>> mapper) {
        return mapBoth(mapper.andThen(Rewrapper::get));
    }

    @Deprecated // todo: fix
    static <K, V> BiStageAdapter<K, V, K, V> distinctValue() {
        return filterValue(new HashSet<>()::add);
    }

    static <K, V> BiStageAdapter<K, V, K, V> peek(BiConsumer<? super K, ? super V> action) {
        return new Map<>(Function.identity(), (k, v) -> {
            action.accept(k, v);
            return v;
        });
    }

    @Deprecated // todo: fix
    static <K, V> BiStageAdapter<K, V, K, V> limit(long limit) {
        return filterValue(new StageAdapter.Structure.Limiter<>(limit));
    }

    @Deprecated // todo: fix
    static <K, V> BiStageAdapter<K, V, K, V> skip(long skip) {
        return filterValue(new StageAdapter.Structure.Skipper<>(skip));
    }

    static <T, X> BiStageAdapter<T, T, X, T> source(final Function<T, X> source) {
        return new BiSource<>(source);
    }

    static <K, V> BiStageAdapter<K, V, K, V> identity() {
        return filterValue(any -> true);
    }

    private final static class Filter<X, Y> extends BiStageAdapter<X, Y, X, Y> {
        private final BiPredicate<@NotNull ? super X, @Nullable ? super Y> filter;

        private Filter(final BiPredicate<@NotNull ? super X, @Nullable ? super Y> filter) {
            super(true, Function.identity(), (k, v) -> filter.test(k, v) ? v : null);
            this.filter = filter;
        }

        @Override
        public KeyedReference<X, Y> advance(final KeyedReference<X, Y> ref) {
            return new KeyedReference.Support.Filtered<>(ref, filter);
        }
    }

    private final static class Map<IX, IY, OX, OY> extends BiStageAdapter<IX, IY, OX, OY> {
        private Map(
                Function<? super IX, ? extends OX> keyMapper,
                Function<? super IY, ? extends OY> valueMapper
        ) {
            super(false, keyMapper, valueMapper);
        }

        private Map(
                Function<? super IX, ? extends OX> keyMapper,
                BiFunction<? super IX, ? super IY, ? extends OY> valueMapper
        ) {
            super(false, keyMapper, valueMapper);
        }

        @Override
        public KeyedReference<OX, OY> advance(KeyedReference<IX, IY> ref) {
            return new KeyedReference.Support.Mapped<>(ref, this::advanceKey, this::advanceValue);
        }
    }

    private static class BiSource<T, X> extends BiStageAdapter<T, T, X, T> {
        private BiSource(Function<@NotNull ? super T, @NotNull ? extends X> source) {
            super(false, source, Function.identity());
        }

        @Override
        public KeyedReference<X, T> advance(KeyedReference<T, T> ref) {
            return new KeyedReference.Support.Base<>(advanceKey(ref.getValue()), ref);
        }
    }
}
