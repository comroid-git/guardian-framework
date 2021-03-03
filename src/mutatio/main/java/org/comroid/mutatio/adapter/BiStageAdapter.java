package org.comroid.mutatio.adapter;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.function.*;

public abstract class BiStageAdapter<InK, InV, OutK, OutV>
        extends ReferenceStageAdapter<InK, OutK, InV, OutV, KeyedReference<InK, InV>, KeyedReference<OutK, OutV>>
        implements KeyedReference.Advancer<InK, InV, OutK, OutV> {
    static <K, V> BiStageAdapter<K, V, K, V> filterKey(Predicate<? super K> predicate) {
        return new Filter<>(predicate, any -> true);
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterValue(Predicate<? super V> predicate) {
        return new Filter<>(any -> true, predicate);
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterBoth(final BiPredicate<? super K, ? super V> predicate) {
        return ref -> KeyedReference.conditional(() -> predicate.test(ref.getKey(), ref.getValue()), ref::getKey, ref);
    }

    static <K, V, R> BiStageAdapter<K, V, R, V> mapKey(Function<? super K, ? extends R> mapper) {
        return new Map<>(mapper, Function.identity());
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> mapValue(Function<? super V, ? extends R> mapper) {
        return new Map<>(Function.identity(), mapper);
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> mapBoth(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return ref -> new KeyedReference.Support.Base<>(ref.getKey(), ref.map(v -> mapper.apply(ref.getKey(), v)));
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
        return ref -> {
            action.accept(ref.getKey(), ref.getValue());
            return ref;
        };
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

    @Override
    public abstract KeyedReference<OutK, OutV> advance(KeyedReference<InK, InV> ref);

    @OverrideOnly
    public OutK convertKey(InK key) {
        return Polyfill.uncheckedCast(key);
    }

    private final static class Filter<X, Y> extends BiStageAdapter<X, Y, X, Y> {
        private final Predicate<@NotNull ? super X> keyFilter;
        private final Predicate<@Nullable ? super Y> valueFilter;

        @Override
        public boolean isIdentityValue() {
            return true;
        }

        private Filter(Predicate<@NotNull ? super X> keyFilter, Predicate<@Nullable ? super Y> valueFilter) {
            this.keyFilter = keyFilter;
            this.valueFilter = valueFilter;
        }

        @Override
        public KeyedReference<X, Y> advance(final KeyedReference<X, Y> ref) {
            return new KeyedReference.Support.Filtered<>(ref, keyFilter, valueFilter);
        }

        @Override
        public X advanceKey(X key) {
            return key;
        }

        @Override
        public Y advanceValue(X key, Y value) {
            return value;
        }
    }

    private final static class Map<IX, IY, OX, OY> extends BiStageAdapter<IX, IY, OX, OY> {
        private final Function<@NotNull ? super IX, @NotNull ? extends OX> keyMapper;
        private final Function<@Nullable ? super IY, @Nullable ? extends OY> valueMapper;

        @Override
        public boolean isIdentityValue() {
            return false;
        }

        private Map(
                Function<@NotNull ? super IX, @NotNull ? extends OX> keyMapper,
                Function<@Nullable ? super IY, @Nullable ? extends OY> valueMapper
        ) {
            this.keyMapper = keyMapper;
            this.valueMapper = valueMapper;
        }

        @Override
        public KeyedReference<OX, OY> advance(KeyedReference<IX, IY> ref) {
            return new KeyedReference.Support.Mapped<>(ref, keyMapper, valueMapper);
        }

        @Override
        public OX advanceKey(IX key) {
            return keyMapper.apply(key);
        }

        @Override
        public OY advanceValue(IX key, IY value) {
            return valueMapper.apply(value);
        }

        @Override
        public OX convertKey(IX key) {
            return keyMapper.apply(key);
        }
    }

    private static class BiSource<T, X> extends BiStageAdapter<T, T, X, T> {
        private final Function<T, X> source;

        @Override
        public boolean isIdentityValue() {
            return true;
        }

        public BiSource(Function<T, X> source) {
            this.source = source;
        }

        @Override
        public KeyedReference<X, T> advance(KeyedReference<T, T> ref) {
            return new KeyedReference.Support.Base<>(source.apply(ref.getValue()), ref);
        }

        @Override
        public X advanceKey(T key) {
            return source.apply(key);
        }

        @Override
        public T advanceValue(T key, T value) {
            return value;
        }

        @Override
        public X convertKey(T value) {
            return source.apply(value);
        }
    }
}
