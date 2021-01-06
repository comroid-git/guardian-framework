package org.comroid.mutatio.ref;

import org.comroid.api.Rewrapper;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.*;

public interface BiProcessor<K, V> extends Processor<V>, KeyedReference<K, V> {
    @Internal
    <R> @Nullable Function<K, R> getKeyReverser();

    @Override
    default BiProcessor<K, V> filter(Predicate<? super V> predicate) {
        return new Support.Base<>(this, getKey(),
                new Processor.Support.Filtered<>(this, predicate));
    }

    default BiProcessor<K, V> filterKey(Predicate<? super K> predicate) {
        return new Support.Base<>(this, getKey(),
                new Processor.Support.Filtered<>(this, me -> predicate.test(getKey())));
    }

    @Override
    default <R> BiProcessor<K, R> map(Function<? super V, ? extends R> mapper) {
        return new Support.Base<>(this, getKey(),
                new Processor.Support.Remapped<>(this, mapper, null));
    }

    default <R> BiProcessor<R, V> mapKey(Function<? super K, ? extends R> mapper, Function<R, K> reverse) {
        return new Support.Base<>(this, mapper.apply(getKey()), reverse, Function.identity(), this);
    }

    default BiProcessor<K, V> peek(BiConsumer<? super K, ? super V> action) {
        return filter(it -> {
            action.accept(getKey(), it);
            return true;
        });
    }

    @Override
    default <R> BiProcessor<K, R> flatMap(final Class<R> type) {
        return filter(type::isInstance).map(type::cast);
    }

    @Override
    default <R> BiProcessor<K, R> flatMap(Function<? super V, ? extends Reference<? extends R>> mapper) {
        return map(mapper.andThen(Reference::get));
    }

    @Override
    default <R> BiProcessor<K, R> flatMapOptional(Function<? super V, ? extends Optional<? extends R>> mapper) {
        return map(mapper.andThen(opt -> opt.orElse(null)));
    }

    <R> Processor<R> merge(BiFunction<? super K, ? super V, ? extends R> mergeFunction);

    final class Support {
        static class Base<K, V> extends Reference.Support.Base<V> implements BiProcessor<K, V> {
            private final K key;
            private final Function<V, ?> valueReverser;
            private final Function<K, ?> keyReverser;
            private final Supplier<V> computor;

            @Override
            public K getKey() {
                return key;
            }

            @Override
            public @Nullable <R> Function<K, R> getKeyReverser() {
                //noinspection unchecked
                return (Function<K, R>) keyReverser;
            }

            @Override
            public <R> Processor<R> merge(final BiFunction<? super K, ? super V, ? extends R> mergeFunction) {
                return new Processor.Support.Remapped<>(
                        this,
                        value -> mergeFunction.apply(getKey(), value),
                        null
                );
            }

            public <OldV> Base(
                    @Nullable KeyedReference<K, OldV> parent,
                    K key,
                    Supplier<V> computor
            ) {
                this(parent, key, null, computor);
            }

            public <OldV> Base(
                    @Nullable KeyedReference<K, OldV> parent,
                    K key,
                    @Nullable Function<V, OldV> valueReverser,
                    Supplier<V> computor
            ) {
                this(parent, key, valueReverser, Function.identity(), computor);
            }

            public <OldK, OldV> Base(
                    @Nullable KeyedReference<OldK, OldV> parent,
                    K key,
                    Function<K, OldK> keyReverser,
                    @Nullable Function<V, OldV> valueReverser,
                    Supplier<V> computor
            ) {
                super(parent, (parent != null && parent.isMutable()) & valueReverser != null);

                this.key = key;
                this.keyReverser = keyReverser;
                this.valueReverser = valueReverser;
                this.computor = computor;
            }

            @Override
            protected V doGet() {
                return computor.get();
            }

            @Override
            protected boolean doSet(V value) {
                //noinspection unchecked
                return Objects.requireNonNull(getParent()
                        .into(KeyedReference.class), "Invalid Parent")
                        .set(valueReverser.apply(value));
            }
        }
    }
}
