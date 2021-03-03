package org.comroid.mutatio.model;

import org.comroid.api.Rewrapper;
import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.adapter.BiStageAdapter;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.adapter.StageAdapter;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferencePipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.comroid.api.Polyfill.uncheckedCast;

public interface RefOPs<K, V, Ref extends Reference<V>> extends Pipeable<V>, UncheckedCloseable {
    default <X, Y> ReferencePipe<K, V, X, Y> addStage(ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter) {
        return addStage(adapter, null, null);
    }

    default <X, Y> ReferencePipe<K, V, X, Y> addStage(
            ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter,
            @Nullable Executor executor
    ) {
        return addStage(adapter, null, executor);
    }

    <X, Y> ReferencePipe<K, V, X, Y> addStage(
            ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter,
            @Nullable Comparator<? super Y> comparator,
            @Nullable Executor executor
    );

    default ReferencePipe<?, ?, K, V> filter(Predicate<? super V> predicate) {
        return addStage(uncheckedCast(StageAdapter.filter(predicate)));
    }

    default ReferencePipe<?, ?, K, V> yield(Predicate<? super V> predicate, Consumer<? super V> elseConsume) {
        return filter(new Structure.YieldAction<>(predicate, elseConsume));
    }

    default <R> ReferencePipe<?, ?, K, R> map(Function<? super V, ? extends R> mapper) {
        return addStage(uncheckedCast(StageAdapter.map(mapper)));
    }

    default <R> ReferencePipe<?, ?, K, R> flatMap(final Class<R> target) {
        return filter(target::isInstance).map(target::cast);
    }

    default <R> ReferencePipe<?, ?, K, R> flatMap(Function<? super V, ? extends Rewrapper<? extends R>> mapper) {
        return addStage(uncheckedCast(StageAdapter.flatMap(mapper)));
    }

    default <R> ReferencePipe<?, ?, K, R> flatMapOptional(Function<? super V, ? extends Optional<? extends R>> mapper) {
        return addStage(uncheckedCast(StageAdapter.flatMap(mapper.andThen(Rewrapper::ofOptional))));
    }

    default ReferencePipe<?, ?, K, V> peek(Consumer<? super V> action) {
        return filter(new Structure.PeekAction<>(action));
    }

    // todo: fix
    @Deprecated
    default ReferencePipe<?, ?, K, V> distinct() {
        return filter(new Structure.DistinctFilter<>());
    }

    // todo: fix
    @Deprecated
    default ReferencePipe<?, ?, K, V> limit(long maxSize) {
        return filter(new Structure.Limiter<>(maxSize));
    }

    // todo: fix
    @Deprecated
    default ReferencePipe<?, ?, K, V> skip(long skip) {
        return filter(new Structure.Skipper<>(skip));
    }

    default ReferencePipe<?, ?, K, V> sorted() {
        return sorted(uncheckedCast(Comparator.naturalOrder()));
    }

    default ReferencePipe<?, ?, K, V> sorted(Comparator<? super V> comparator) {
        return addStage(uncheckedCast(BiStageAdapter.identity()), comparator, null);
    }

    @NotNull
    default KeyedReference<K, V> findFirst() {
        //noinspection unchecked
        return ((RefContainer<K, V, KeyedReference<K, V>>) this).streamRefs()
                .findFirst()
                .orElseGet(KeyedReference::emptyKey);
    }

    @NotNull
    default KeyedReference<K, V> findAny() {
        //noinspection unchecked
        return ((RefContainer<K, V, KeyedReference<K, V>>) this).streamRefs()
                .findAny()
                .orElseGet(KeyedReference::emptyKey);
    }
}
