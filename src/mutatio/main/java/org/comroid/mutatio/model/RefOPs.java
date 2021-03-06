package org.comroid.mutatio.model;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.adapter.BiStageAdapter;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferencePipe;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.*;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.uncheckedCast;

public interface RefOPs<K, V, Ref extends Reference<V>> extends UncheckedCloseable, Pipeable<V> {
    @Nullable
    default V getFirst() {
        return findFirst().get();
    }

    @Nullable
    default V getAny() {
        return findAny().get();
    }

    default <X, Y> ReferencePipe<K, V, X, Y> addStage(ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter) {
        return addStage(adapter, null);
    }

    default <X, Y> ReferencePipe<K, V, X, Y> addStage(
            ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter,
            @Nullable Executor executor
    ) {
        return addStage(adapter, null, executor);
    }

    <X, Y> ReferencePipe<K, V, X, Y> addStage(
            ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter,
            @Nullable Comparator<KeyedReference<X, Y>> comparator,
            @Nullable Executor executor
    );

    @Deprecated
    default <R> ReferencePipe<?, ?, R, V> bi(Function<? super V, ? extends R> source) {
        return overwriteKey(source);
    }

    default <R> ReferencePipe<?, ?, R, V> overwriteKey(Function<? super V, ? extends R> source) {
        return addStage(uncheckedCast(BiStageAdapter.source(source)));
    }

    default ReferencePipe<?, ?, K, V> filter(Predicate<? super V> predicate) {
        return addStage(uncheckedCast(BiStageAdapter.filterValue(predicate)));
    }

    default ReferencePipe<?, ?, K, V> filterKey(Predicate<? super K> predicate) {
        return addStage(uncheckedCast(BiStageAdapter.filterKey(predicate)));
    }

    default ReferencePipe<?, ?, K, V> filterBoth(BiPredicate<? super K, ? super V> predicate) {
        return addStage(uncheckedCast(BiStageAdapter.filterBoth(predicate)));
    }

    default ReferencePipe<?, ?, K, V> yield(Predicate<? super V> predicate, Consumer<? super V> elseConsume) {
        return filter(new Structure.YieldAction<>(predicate, elseConsume));
    }

    default ReferencePipe<?, ?, K, V> yieldKey(Predicate<? super K> predicate, Consumer<? super K> elseConsume) {
        return filterKey(new Structure.YieldAction<>(predicate, elseConsume));
    }

    default <R> ReferencePipe<?, ?, K, R> map(Function<? super V, ? extends R> mapper) {
        return addStage(uncheckedCast(BiStageAdapter.mapValue(mapper)));
    }

    default <R> ReferencePipe<?, ?, R, V> mapKey(Function<? super K, ? extends R> mapper) {
        return addStage(uncheckedCast(BiStageAdapter.mapKey(mapper)));
    }

    default <R> ReferencePipe<?, ?, K, R> mapBoth(BiFunction<? super K, ? super V, ? extends R> mapper) {
        return addStage(uncheckedCast(BiStageAdapter.mapBoth(mapper)));
    }

    default <R> ReferencePipe<?, ?, K, R> flatMap(final Class<R> target) {
        return filter(target::isInstance).map(target::cast);
    }

    default <R> ReferencePipe<?, ?, K, R> flatMap(Function<? super V, ? extends Rewrapper<? extends R>> mapper) {
        return addStage(uncheckedCast(BiStageAdapter.flatMapValue(mapper)));
    }

    default <R> ReferencePipe<?, ?, K, R> flatMapOptional(Function<? super V, ? extends Optional<? extends R>> mapper) {
        return flatMap(mapper.andThen(Rewrapper::ofOptional));
    }

    // todo inspect
    @Experimental
    default <R> ReferencePipe<?, ?, K, R> flatMapStream(Function<? super V, ? extends Stream<? extends R>> mapper) {
        return flatMap(mapper.andThen(stream -> stream.findAny().map(Reference::constant).orElseGet(Reference::empty)));
    }

    default <R> ReferencePipe<?, ?, R, V> flatMapKey(final Class<R> target) {
        return filterKey(target::isInstance).mapKey(target::cast);
    }

    default <R> ReferencePipe<?, ?, R, V> flatMapKey(Function<? super K, ? extends Rewrapper<? extends R>> mapper) {
        return addStage(uncheckedCast(BiStageAdapter.flatMapKey(mapper)));
    }

    default <R> ReferencePipe<?, ?, R, V> flatMapKeyOptional(Function<? super K, ? extends Optional<? extends R>> mapper) {
        return flatMapKey(mapper.andThen(Rewrapper::ofOptional));
    }

    default <R> ReferencePipe<?, ?, K, R> flatMapBoth(BiFunction<? super K, ? super V, ? extends Rewrapper<? extends R>> mapper) {
        return addStage(uncheckedCast(BiStageAdapter.flatMapBoth(mapper)));
    }

    default <R> ReferencePipe<?, ?, K, R> flatMapBothOptional(BiFunction<? super K, ? super V, ? extends Optional<? extends R>> mapper) {
        return flatMapBoth(mapper.andThen(Rewrapper::ofOptional));
    }

    default ReferencePipe<?, ?, K, V> peek(Consumer<? super V> action) {
        return filter(new Structure.PeekAction<>(action));
    }

    default ReferencePipe<?, ?, K, V> peekKey(Consumer<? super K> action) {
        return filterKey(new Structure.PeekAction<>(action));
    }

    default ReferencePipe<?, ?, K, V> peekBoth(BiConsumer<? super K, ? super V> action) {
        return filterBoth((k, v) -> {
            action.accept(k, v);
            return true;
        });
    }

    // todo: fix
    @Deprecated
    default ReferencePipe<?, ?, K, V> distinct() {
        return filter(new Structure.DistinctFilter<>());
    }

    // todo: fix
    @Deprecated
    default ReferencePipe<?, ?, K, V> distinctKey() {
        return filterKey(new Structure.DistinctFilter<>());
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
        return sortedRef(Structure.wrapComparator(comparator));
    }

    default ReferencePipe<?, ?, K, V> sortedRef(Comparator<KeyedReference<K, V>> comparator) {
        return addStage(uncheckedCast(BiStageAdapter.identity()), comparator, null);
    }

    @NotNull
    default KeyedReference<K, V> findFirst() {
        // todo This is wrong
        return ((RefContainer<K, V>) this).streamRefs()
                .findFirst()
                .orElseGet(KeyedReference::emptyKey);
    }

    @NotNull
    default KeyedReference<K, V> findAny() {
        // todo This is wrong
        return ((RefContainer<K, V>) this).streamRefs()
                .findAny()
                .orElseGet(KeyedReference::emptyKey);
    }

    default CompletableFuture<V> next() {
        class OnceCompletingStage extends BiStageAdapter<V, V, V, V> {
            private final CompletableFuture<V> future = new CompletableFuture<>();

            protected OnceCompletingStage() {
                super(true, Function.identity(), (v, v2) -> v, Function.identity(), Function.identity());
            }

            @Override
            public KeyedReference<V, V> advance(KeyedReference<V, V> ref) {
                if (!ref.isNull() && !future.isDone()) {
                    future.complete(ref.get());
                    return ref;
                }
                return null;
            }
        }

        final OnceCompletingStage stage = new OnceCompletingStage();
        final RefOPs<V, V, KeyedReference<V, V>> resulting = addStage(Polyfill.uncheckedCast(stage));
        stage.future.thenRun(resulting::close);

        return stage.future;
    }
}
