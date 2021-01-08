package org.comroid.mutatio.pipe;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.function.*;

public interface BiStageAdapter<InK, InV, OutK, OutV>
        extends StageAdapter<InV, OutV, KeyedReference<InK, InV>, KeyedReference<OutK, OutV>> {
    default boolean isIdentity() {
        return false;
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterKey(Predicate<? super K> predicate) {
        return new Support.Filter<>(predicate, any -> true);
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterValue(Predicate<? super V> predicate) {
        return new Support.Filter<>(any -> true, predicate);
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterBoth(final BiPredicate<? super K, ? super V> predicate) {
        return ref -> KeyedReference.conditional(() -> predicate.test(ref.getKey(), ref.getValue()), ref::getKey, ref);
    }

    static <K, V, R> BiStageAdapter<K, V, R, V> mapKey(
            Function<? super K, ? extends R> mapper,
            Function<? super R, ? extends K> keyReverser
    ) {
        return new Support.Map<>(mapper, keyReverser, Function.identity());
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> mapValue(Function<? super V, ? extends R> mapper) {
        return new Support.Map<>(Function.identity(), Function.identity(), mapper);
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> mapBoth(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return ref -> KeyedReference.conditional(
                () -> true,
                ref::getKey,
                () -> mapper.apply(ref.getKey(), ref.getValue())
        );
    }

    static <K, V, R> BiStageAdapter<K, V, R, V> flatMapKey(
            Function<? super K, ? extends Rewrapper<? extends R>> mapper,
            Function<? super R, ? extends K> keyReverser
    ) {
        return new Support.Map<>(mapper.andThen(Rewrapper::get), keyReverser, Function.identity());
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> flatMapValue(Function<? super V, ? extends Rewrapper<? extends R>> mapper) {
        return new Support.Map<>(Function.identity(), Function.identity(), mapper.andThen(Rewrapper::get));
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> flatMapBoth(BiFunction<? super K, ? super V, ? extends Rewrapper<? extends R>> mapper) {
        return ref -> KeyedReference.conditional(
                () -> true,
                ref::getKey,
                () -> mapper.andThen(Rewrapper::get).apply(ref.getKey(), ref.getValue())
        );
    }

    static <K, V> BiStageAdapter<K, V, K, V> distinctValue() {
        return filterValue(new HashSet<>()::add);
    }

    static <K, V> BiStageAdapter<K, V, K, V> peek(BiConsumer<? super K, ? super V> action) {
        return ref -> {
            action.accept(ref.getKey(), ref.getValue());
            return ref;
        };
    }

    static <K, V> BiStageAdapter<K, V, K, V> limit(long limit) {
        return filterValue(new Structure.Limiter<>(limit));
    }

    static <K, V> BiStageAdapter<K, V, K, V> skip(long skip) {
        return filterValue(new Structure.Skipper<>(skip));
    }

    static <T, X> BiStageAdapter<T, T, X, T> source(final Function<T, X> source) {
        return new Support.BiSource<>(source);
    }

    @Override
    KeyedReference<OutK, OutV> advance(KeyedReference<InK, InV> ref);

    @OverrideOnly
    default OutK convertKey(InK key) {
        if (isIdentity())
            return Polyfill.uncheckedCast(key);
        throw new AbstractMethodError();
    }

    @OverrideOnly
    default InK reverseKey(OutK key) {
        if (isIdentity())
            return Polyfill.uncheckedCast(key);
        throw new AbstractMethodError();
    }

    final class Support {
        public final static class Filter<X, Y> implements BiStageAdapter<X, Y, X, Y> {
            private final Predicate<@NotNull ? super X> keyFilter;
            private final Predicate<@Nullable ? super Y> valueFilter;

            @Override
            public boolean isIdentity() {
                return true;
            }

            private Filter(Predicate<@NotNull ? super X> keyFilter, Predicate<@Nullable ? super Y> valueFilter) {
                this.keyFilter = keyFilter;
                this.valueFilter = valueFilter;
            }

            @Override
            public KeyedReference<X, Y> advance(KeyedReference<X, Y> ref) {
                if (keyFilter.test(ref.getKey()) && ref.test(valueFilter))
                    return ref;
                return null;
            }
        }

        public final static class Map<IX, IY, OX, OY> implements BiStageAdapter<IX, IY, OX, OY> {
            private final Function<@NotNull ? super IX, @NotNull ? extends OX> keyMapper;
            private final Function<@NotNull ? super OX, @NotNull ? extends IX> keyReverser;
            private final Function<@Nullable ? super IY, @Nullable ? extends OY> valueMapper;

            private Map(
                    Function<@NotNull ? super IX, @NotNull ? extends OX> keyMapper,
                    Function<@NotNull ? super OX, @NotNull ? extends IX> keyReverser,
                    Function<@Nullable ? super IY, @Nullable ? extends OY> valueMapper
            ) {
                this.keyMapper = keyMapper;
                this.keyReverser = keyReverser;
                this.valueMapper = valueMapper;
            }

            @Override
            public KeyedReference<OX, OY> advance(KeyedReference<IX, IY> ref) {
                return new ResultingKeyedReference<>(
                        keyMapper.apply(ref.getKey()),
                        ref.map(valueMapper)
                );
            }

            @Override
            public OX convertKey(IX key) {
                return keyMapper.apply(key);
            }

            @Override
            public IX reverseKey(OX key) {
                return keyReverser.apply(key);
            }

            @Override
            public OY convertValue(IY value) {
                return valueMapper.apply(value);
            }
        }

        public static class BiSource<T, X> implements BiStageAdapter<T, T, X, T> {
            private final Function<T, X> source;

            public BiSource(Function<T, X> source) {
                this.source = source;
            }

            @Override
            public KeyedReference<X, T> advance(KeyedReference<T, T> ref) {
                return KeyedReference.conditional(() -> true, () -> ref.into(source), ref);
            }

            @Override
            public T convertValue(T value) {
                return value;
            }

            @Override
            public X convertKey(T value) {
                return source.apply(value);
            }
        }

        private final static class ResultingKeyedReference<K, V> extends KeyedReference.Support.Base<K, V> {
            @Override
            public boolean isMutable() {
                return false;
            }

            public ResultingKeyedReference(K key, Reference<V> valueHolder) {
                super(key, valueHolder);
            }
        }
    }
}