package org.comroid.mutatio.adapter;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.model.Structure;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.stack.OutputStack;
import org.comroid.mutatio.stack.RefStack;
import org.comroid.mutatio.stack.RefStackUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

@ApiStatus.Experimental // todo: fix
public abstract class BiStageAdapter<InK, InV, OutK, OutV>
        extends ReferenceStageAdapter<InK, OutK, InV, OutV, KeyedReference<InK, InV>, KeyedReference<OutK, OutV>>
        implements KeyedReference.Advancer<InK, InV, OutK, OutV> {
    protected BiStageAdapter(
            boolean isFiltering,
            Function<? super InK, ? extends OutK> keyMapper,
            final Function<? super InV, ? extends OutV> valueMapper,
            @Nullable Function<? super OutK, ? extends InK> keyReverser,
            @Nullable Function<? super OutV, ? extends InV> valueReverser
    ) {
        this(isFiltering, keyMapper, (unused, in) -> valueMapper.apply(in), keyReverser, valueReverser);
    }

    protected BiStageAdapter(
            boolean isFiltering,
            Function<? super InK, ? extends OutK> keyMapper,
            BiFunction<? super InK, ? super InV, ? extends OutV> valueMapper,
            @Nullable Function<? super OutK, ? extends InK> keyReverser,
            @Nullable Function<? super OutV, ? extends InV> valueReverser
    ) {
        super(isFiltering, keyMapper, valueMapper, keyReverser, valueReverser);
    }

    public static <K, V> BiStageAdapter<K, V, K, V> filterKey(final Predicate<? super K> predicate) {
        return filterBoth((k, unused) -> predicate.test(k));
    }

    public static <K, V> BiStageAdapter<K, V, K, V> filterValue(final Predicate<? super V> predicate) {
        return filterBoth((unused, v) -> predicate.test(v));
    }

    public static <K, V> BiStageAdapter<K, V, K, V> filterBoth(final BiPredicate<? super K, ? super V> predicate) {
        return new Filter<>(predicate);
    }

    public static <K, V, R> BiStageAdapter<K, V, R, V> mapKey(Function<? super K, ? extends R> mapper) {
        return new Map<>(mapper, Function.identity());
    }

    public static <K, V, R> BiStageAdapter<K, V, K, R> mapValue(Function<? super V, ? extends R> mapper) {
        return new Map<>(Function.identity(), mapper);
    }

    public static <K, V, R> BiStageAdapter<K, V, K, R> mapBoth(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return new Map<>(Function.identity(), mapper);
    }

    public static <K, V, R> BiStageAdapter<K, V, R, V> flatMapKey(Function<? super K, ? extends Rewrapper<? extends R>> mapper) {
        return new Map<>(mapper.andThen(Rewrapper::get), Function.identity());
    }

    public static <K, V, R> BiStageAdapter<K, V, K, R> flatMapValue(Function<? super V, ? extends Rewrapper<? extends R>> mapper) {
        return new Map<>(Function.identity(), mapper.andThen(Rewrapper::get));
    }

    public static <K, V, R> BiStageAdapter<K, V, K, R> flatMapBoth(BiFunction<? super K, ? super V, ? extends Rewrapper<? extends R>> mapper) {
        return mapBoth(mapper.andThen(Rewrapper::get));
    }

    @Deprecated // todo: fix
    public static <K, V> BiStageAdapter<K, V, K, V> distinctValue() {
        return filterValue(new HashSet<>()::add);
    }

    public static <K, V> BiStageAdapter<K, V, K, V> peek(BiConsumer<? super K, ? super V> action) {
        return new Map<>(Function.identity(), (k, v) -> {
            action.accept(k, v);
            return v;
        });
    }

    @Deprecated // todo: fix
    public static <K, V> BiStageAdapter<K, V, K, V> limit(long limit) {
        return filterValue(new Structure.Limiter<>(limit));
    }

    @Deprecated // todo: fix
    public static <K, V> BiStageAdapter<K, V, K, V> skip(long skip) {
        return filterValue(new Structure.Skipper<>(skip));
    }

    public static <T, X> BiStageAdapter<?, T, X, T> source(final Function<? super T, ? extends X> source) {
        return new BiSource<>(source);
    }

    public static <K, V> BiStageAdapter<K, V, K, V> identity() {
        return filterValue(any -> true);
    }

    @Override
    public final String toString() {
        return String.format("BiStageAdapter(%s)@%d", getClass().getSimpleName(), hashCode());
    }

    private final static class Filter<X, Y> extends BiStageAdapter<X, Y, X, Y> {
        private final BiPredicate<@NotNull ? super X, @Nullable ? super Y> filter;

        private Filter(final BiPredicate<@NotNull ? super X, @Nullable ? super Y> filter) {
            super(true, Function.identity(), (k, v) -> filter.test(k, v) ? v : null, Function.identity(), Function.identity());
            this.filter = filter;
        }

        @Override
        public KeyedReference<X, Y> advance(final KeyedReference<X, Y> ref) {
            class BiFilterStack extends OutputStack<Y> {
                private final RefStack<X> keyStack = ref.keyStack();
                private final RefStack<Y> valueStack = ref.valueStack();

                protected BiFilterStack() {
                    super(ref.valueStack(), "KeyedReference.filter()");
                }

                @Override
                protected Y $get() {
                    if (keyStack.accumulate(valueStack, filter::test))
                        return valueStack.get();
                    return null;
                }
            }
            BiFilterStack stack = new BiFilterStack();
            return new KeyedReference<>(stack.keyStack, stack);
        }
    }

    private final static class Map<IX, IY, OX, OY> extends BiStageAdapter<IX, IY, OX, OY> {
        private Map(
                Function<? super IX, ? extends OX> keyMapper,
                Function<? super IY, ? extends OY> valueMapper
        ) {
            this(keyMapper, valueMapper, null, null);
        }

        private Map(
                Function<? super IX, ? extends OX> keyMapper,
                Function<? super IY, ? extends OY> valueMapper,
                @Nullable Function<? super OX, ? extends IX> keyReverser,
                @Nullable Function<? super OY, ? extends IY> valueReverser
        ) {
            super(false, keyMapper, valueMapper, keyReverser, valueReverser);
        }

        private Map(
                Function<? super IX, ? extends OX> keyMapper,
                BiFunction<? super IX, ? super IY, ? extends OY> valueMapper
        ) {
            this(keyMapper, valueMapper, null, null);
        }

        private Map(
                Function<? super IX, ? extends OX> keyMapper,
                BiFunction<? super IX, ? super IY, ? extends OY> valueMapper,
                @Nullable Function<? super OX, ? extends IX> keyReverser,
                @Nullable Function<? super OY, ? extends IY> valueReverser
        ) {
            super(false, keyMapper, valueMapper, keyReverser, valueReverser);
        }

        @Override
        public KeyedReference<OX, OY> advance(KeyedReference<IX, IY> ref) {
            class MergeValueStack extends OutputStack<OY> {
                private final RefStack<IX> keyStack = ref.keyStack();
                private final RefStack<IY> valueStack = ref.valueStack();

                protected MergeValueStack() {
                    super(ref.valueStack(), "KeyedReference.map()[merge]");
                }

                @Override
                protected OY $get() {
                    return keyStack.accumulate(valueStack, valueMapper::apply);
                }
            }
            MergeValueStack stack = new MergeValueStack();
            return new KeyedReference<>(RefStackUtil.$map(stack.keyStack, keyMapper), stack);
        }
    }

    @Internal
    public static final class BiSource<T, X> extends BiStageAdapter<Object, T, X, T> {
        @Internal
        public final Function<@NotNull ? super T, @NotNull ? extends X> source;
        private final ConcurrentHashMap<X, Object> reverseKeys = new ConcurrentHashMap<>();

        private BiSource(Function<@NotNull ? super T, @NotNull ? extends X> source) {
            super(false, Polyfill::uncheckedCast, (BiFunction<? super Object, ? super T, ? extends T>) (a, b) -> b, null, Function.identity());
            this.source = source;
        }

        @Override
        public KeyedReference<X, T> advance(KeyedReference<Object, T> ref) {
            RefStack<X> stack = RefStackUtil.$map(ref.valueStack(), source);
            return new KeyedReference<>(stack, ref.valueStack());
        }

        @Override
        public Optional<Object> revertKey(X key) {
            return Optional.ofNullable(reverseKeys.getOrDefault(key, null));
        }
    }
}
