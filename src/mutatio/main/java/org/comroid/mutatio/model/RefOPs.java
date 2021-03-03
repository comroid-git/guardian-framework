package org.comroid.mutatio.model;

import org.comroid.api.Rewrapper;
import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.adapter.BiStageAdapter;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
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

public interface RefOPs<K, V, Ref extends Reference<V>> extends Pipeable<V>, UncheckedCloseable {
    default <X, Y> RefOPs<X, Y, KeyedReference<X, Y>> addStage(ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter) {
        return addStage(adapter, null);
    }

    <X, Y> ReferencePipe<K, V, X, Y> addStage(ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter, @Nullable Executor executor);

    default ReferencePipe<?, ?, K, V> filter(Predicate<? super V> predicate) {
        return addStage(BiStageAdapter.<K, V>filterValue(predicate));
    }

    default ReferencePipe<?, ?, K, V> yield(Predicate<? super V> predicate, Consumer<? super V> elseConsume) {
        return null;
    }

    default <R> ReferencePipe<?, ?, K, R> map(Function<? super V, ? extends R> mapper) {
        return null;
    }

    default <R> ReferencePipe<?, ?, K, R> flatMap(Class<R> target) {
        return null;
    }

    default <R> ReferencePipe<?, ?, K, R> flatMap(Function<? super V, ? extends Rewrapper<? extends R>> mapper) {
        return null;
    }

    default <R> ReferencePipe<?, ?, K, R> flatMapOptional(Function<? super V, ? extends Optional<? extends R>> mapper) {
        return null;
    }

    default ReferencePipe<?, ?, K, V> peek(Consumer<? super V> action) {
        return null;
    }

    // todo: fix
    @Deprecated
    default ReferencePipe<?, ?, K, V> distinct() {
        return null;
    }

    // todo: fix
    @Deprecated
    default ReferencePipe<?, ?, K, V> limit(long maxSize) {
        return null;
    }

    // todo: fix
    @Deprecated
    default ReferencePipe<?, ?, K, V> skip(long skip) {
        return null;
    }

    default ReferencePipe<?, ?, K, V> sorted() {
        return null;
    }

    default ReferencePipe<?, ?, K, V> sorted(Comparator<? super V> comparator) {
        return null;
    }

    @NotNull
    default Reference<V> findFirst() {
        return null;
    }

    @NotNull
    default Reference<V> findAny() {
        return null;
    }
}
