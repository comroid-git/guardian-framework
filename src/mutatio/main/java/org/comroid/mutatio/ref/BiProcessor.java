package org.comroid.mutatio.ref;

import java.util.Optional;
import java.util.function.*;

public interface BiProcessor<K, V> extends Processor<V> {
    @Override
    BiProcessor<K, V> filter(Predicate<? super V> predicate);

    BiProcessor<K, V> filterKey(Predicate<? super K> predicate);

    @Override
    <R> BiProcessor<K, R> map(Function<? super V, ? extends R> mapper);

    <R> BiProcessor<R, V> mapKey(Function<? super K, ? extends R> mapper);

    BiProcessor<K, V> peek(BiConsumer<? super K, ? super V> action);

    @Override
    <R> BiProcessor<K, R> flatMap(Class<R> type);

    @Override
    <R> BiProcessor<K, R> flatMap(Function<? super V, ? extends Reference<? extends R>> mapper);
    @Override
    <R> BiProcessor<K, R> flatMapOptional(Function<? super V, ? extends Optional<? extends R>> mapper);

    <R> BiProcessor<R, V> flatMapKey(Class<R> type);

    <R> BiProcessor<R, V> flatMapKey(Function<? super K, ? extends Reference<? extends R>> mapper);

    <R> Processor<R> merge(BiFunction<? super K, ? super V, ? extends R> mergeFunction);

    final class Support {
        private final static class Base<K, V>
    }
}
