package org.comroid.mutatio.pipe;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.function.*;

public interface BiStageAdapter<InX, InY, OutX, OutY>
        extends StageAdapter<InY, OutY, KeyedReference<InX, InY>, KeyedReference<OutX, OutY>> {
    static <K, V> BiStageAdapter<K, V, K, V> filterKey(Predicate<? super K> predicate) {
        return new Support.Filter<>(predicate, any -> true);
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterValue(Predicate<? super V> predicate) {
        return new Support.Filter<>(any -> true, predicate);
    }

    static <K, V> BiStageAdapter<K, V, K, V> filterBoth(final BiPredicate<? super K, ? super V> predicate) {
        return ref -> KeyedReference.conditional(() -> predicate.test(ref.getKey(), ref.getValue()), ref::getKey, ref);
    }

    static <K, V, R> BiStageAdapter<K, V, R, V> mapKey(Function<? super K, ? extends R> mapper) {
        return new Support.Map<>(mapper, Function.identity());
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> mapValue(Function<? super V, ? extends R> mapper) {
        return new Support.Map<>(Function.identity(), mapper);
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> mapBoth(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return ref -> KeyedReference.conditional(
                () -> true,
                ref::getKey,
                () -> mapper.apply(ref.getKey(), ref.getValue())
        );
    }

    static <K, V, R> BiStageAdapter<K, V, R, V> flatMapKey(Function<? super K, ? extends Rewrapper<? extends R>> mapper) {
        return new Support.Map<>(mapper.andThen(Rewrapper::get), Function.identity());
    }

    static <K, V, R> BiStageAdapter<K, V, K, R> flatMapValue(Function<? super V, ? extends Rewrapper<? extends R>> mapper) {
        return new Support.Map<>(Function.identity(), mapper.andThen(Rewrapper::get));
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

    @Override
    KeyedReference<OutX, OutY> advance(KeyedReference<InX, InY> ref);

    final class Support {
        public final static class Filter<X, Y> implements BiStageAdapter<X, Y, X, Y> {
            private final Predicate<@NotNull ? super X> keyFilter;
            private final Predicate<@Nullable ? super Y> valueFilter;

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
            private final Function<@Nullable ? super IY, @Nullable ? extends OY> valueMapper;

            private Map(
                    Function<@NotNull ? super IX, @NotNull ? extends OX> keyMapper,
                    Function<@Nullable ? super IY, @Nullable ? extends OY> valueMapper
            ) {
                this.keyMapper = keyMapper;
                this.valueMapper = valueMapper;
            }

            @Override
            public KeyedReference<OX, OY> advance(KeyedReference<IX, IY> ref) {
                return new ResultingKeyedReference<>(
                        keyMapper.apply(ref.getKey()),
                        ref.map(valueMapper)
                );
            }
        }

        private final static class ResultingKeyedReference<K, V> extends KeyedReference.Support.Base<K, V> {
            public ResultingKeyedReference(K key, Reference<V> valueHolder) {
                super(key, valueHolder);
            }

            @Override
            public boolean isMutable() {
                return false;
            }
        }
    }
}
