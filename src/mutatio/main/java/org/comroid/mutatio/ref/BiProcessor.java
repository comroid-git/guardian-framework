package org.comroid.mutatio.ref;

import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.*;

public final class BiProcessor<K, V> extends KeyedReference<K, V> {
    private final K key;
    private final Function<V, ?> valueReverser;
    private final Function<K, ?> keyReverser;
    private final Supplier<V> computor;

    @Override
    public K getKey() {
        return key;
    }

    @Internal
    public @Nullable <R> Function<K, R> getKeyReverser() {
        //noinspection unchecked
        return (Function<K, R>) keyReverser;
    }

    public <OldV> BiProcessor(
            @Nullable KeyedReference<K, OldV> parent,
            K key,
            Reference<V> computor
    ) {
        this(parent, key, null, computor);
    }

    public <OldV> BiProcessor(
            @Nullable KeyedReference<K, OldV> parent,
            K key,
            @Nullable Function<V, OldV> valueReverser,
            Reference<V> computor
    ) {
        this(parent, key, Function.identity(), valueReverser, computor);
    }

    public <OldK, OldV> BiProcessor(
            @Nullable KeyedReference<OldK, OldV> parent,
            K key,
            Function<K, OldK> keyReverser,
            @Nullable Function<V, OldV> valueReverser,
            Reference<V> computor
    ) {
        super(key, computor);

        this.key = key;
        this.keyReverser = keyReverser;
        this.valueReverser = valueReverser;
        this.computor = computor;
    }

    @Override
    public BiProcessor<K, V> filter(Predicate<? super V> predicate) {
        return new BiProcessor<>(this, getKey(),
                new Reference.Support.Filtered<>(this, predicate));
    }

    public BiProcessor<K, V> filterKey(Predicate<? super K> predicate) {
        return new BiProcessor<>(this, getKey(),
                new Reference.Support.Filtered<>(this, me -> predicate.test(getKey())));
    }

    @Override
    public <R> BiProcessor<K, R> map(Function<? super V, ? extends R> mapper) {
        return new BiProcessor<>(this, getKey(),
                new Reference.Support.Remapped<>(this, mapper, null));
    }

    public <R> BiProcessor<R, V> mapKey(Function<? super K, ? extends R> mapper, Function<R, K> reverse) {
        return new BiProcessor<>(this, mapper.apply(getKey()), reverse, Function.identity(), this);
    }

    public BiProcessor<K, V> peek(BiConsumer<? super K, ? super V> action) {
        return filter(it -> {
            action.accept(getKey(), it);
            return true;
        });
    }

    @Override
    public <R> BiProcessor<K, R> flatMap(final Class<R> type) {
        return filter(type::isInstance).map(type::cast);
    }

    @Override
    public <R> BiProcessor<K, R> flatMap(Function<? super V, ? extends Reference<? extends R>> mapper) {
        return map(mapper.andThen(Reference::get));
    }

    @Override
    public <R> BiProcessor<K, R> flatMapOptional(Function<? super V, ? extends Optional<? extends R>> mapper) {
        return map(mapper.andThen(opt -> opt.orElse(null)));
    }

    public <R> Reference<R> merge(final BiFunction<? super K, ? super V, ? extends R> mergeFunction) {
        return new Reference.Support.Remapped<>(
                this,
                value -> mergeFunction.apply(getKey(), value),
                null
        );
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
