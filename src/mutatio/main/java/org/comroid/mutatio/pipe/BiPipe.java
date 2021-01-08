package org.comroid.mutatio.pipe;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;

import java.util.Comparator;
import java.util.function.*;
import java.util.stream.Stream;

public interface BiPipe<K, V> extends Pipe<V> {
    @Override
    Stream<KeyedReference<K, V>> streamRefs();

    <Rk, Rv> BiPipe<Rk, Rv> addBiStage(BiStageAdapter<K, V, Rk, Rv> stage);

    default BiPipe<K, V> filterKey(Predicate<? super K> predicate) {
        return addBiStage(BiStageAdapter.filterKey(predicate));
    }

    @Override
    default BiPipe<K, V> filter(Predicate<? super V> predicate) {
        return addBiStage(BiStageAdapter.filterValue(predicate));
    }

    default BiPipe<K, V> filterBoth(BiPredicate<? super K, ? super V> biPredicate) {
        return addBiStage(BiStageAdapter.filterBoth(biPredicate));
    }

    default <R> BiPipe<R, V> mapKey(Function<? super K, ? extends R> mapper) {
        return addBiStage(BiStageAdapter.mapKey(mapper));
    }

    @Override
    default <R> BiPipe<K, R> map(Function<? super V, ? extends R> mapper) {
        return addBiStage(BiStageAdapter.mapValue(mapper));
    }

    default <R> BiPipe<K, R> mapBoth(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return addBiStage(BiStageAdapter.mapBoth(mapper));
    }

    default <R> BiPipe<R, V> flatMapKey(final Class<R> target) {
        return filterKey(target::isInstance).mapKey(target::cast);
    }

    @Override
    default <R> BiPipe<K, R> flatMap(final Class<R> target) {
        return filter(target::isInstance).map(target::cast);
    }

    default <R> BiPipe<R, V> flatMapKey(Function<? super K, ? extends Rewrapper<? extends R>> mapper) {
        return addBiStage(BiStageAdapter.flatMapKey(mapper));
    }

    @Override
    default <R> BiPipe<K, R> flatMap(Function<? super V, ? extends Rewrapper<? extends R>> mapper) {
        return addBiStage(BiStageAdapter.flatMapValue(mapper));
    }

    default <R> BiPipe<K, R> flatMapBoth(BiFunction<? super K, ? super V, ? extends Rewrapper<? extends R>> mapper) {
        return addBiStage(BiStageAdapter.flatMapBoth(mapper));
    }

    default BiPipe<K, V> peek(BiConsumer<? super K, ? super V> action) {
        return addBiStage(BiStageAdapter.peek(action));
    }

    default ReferenceMap<K, V> distinctKeys() {
        return null; // todo
    }

    @Override
    default BiPipe<K, V> distinct() {
        return addBiStage(BiStageAdapter.distinctValue());
    }

    default <R> Pipe<R> merge(BiFunction<? super K, ? super V, ? extends R> merger) {
        return null; // todo
    }

    @Override
    default BiPipe<K, V> limit(long maxSize) {
        return addBiStage(BiStageAdapter.limit(maxSize));
    }

    @Override
    default BiPipe<K, V> skip(long skip) {
        return addBiStage(BiStageAdapter.skip(skip));
    }

    @Override
    default BiPipe<K, V> sorted() {
        return null; // todo
    }

    @Override
    default BiPipe<K, V> sorted(Comparator<? super V> comparator) {
        return null; // todo
    }

    default void forEach(BiConsumer<? super K, ? super V> action) {
        addBiStage(BiStageAdapter.peek(action)).unwrap();
    }

    @Override
    default Pipe<V> pipe() {
        return this;
    }

    @Override
    default Stream<V> stream() {
        return unwrap().stream();
    }

    @Override
    KeyedReference<K, V> getReference(int index);
}
