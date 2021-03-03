package org.comroid.mutatio.model;

import org.comroid.api.Rewrapper;
import org.comroid.api.UncheckedCloseable;
import org.comroid.mutatio.adapter.BiStageAdapter;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface RefOPs<K, V, Ref extends Reference<V>> extends Pipeable<V>, UncheckedCloseable {
    default<X, Y> RefOPs<X, Y, KeyedReference<X, Y>> addStage(ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter) {
        return addStage(adapter, null);
    }

    <X, Y> RefOPs<X, Y, KeyedReference<X, Y>> addStage(ReferenceStageAdapter<K, X, V, Y, Ref, KeyedReference<X, Y>> adapter, @Nullable Executor executor);

    default RefOPs<K, V, Ref> filter(Predicate<? super V> predicate) {
        return addStage(BiStageAdapter.<K, V>filterValue(predicate));
    }

    default RefOPs<K, V, Ref> yield(Predicate<? super V> predicate, Consumer<? super V> elseConsume) {
        return null;
    }

    default <R> RefOPs<K, R, ? extends Reference<R>> map(Function<? super V, ? extends R> mapper) {
        return null;
    }

    default <R> RefOPs<K, R, ? extends Reference<R>> flatMap(Class<R> target) {
        return null;
    }

    default <R> RefOPs<K, R, ? extends Reference<R>> flatMap(Function<? super V, ? extends Rewrapper<? extends R>> mapper) {
        return null;
    }

    default <R> RefOPs<K, R, ? extends Reference<R>> flatMapOptional(Function<? super V, ? extends Optional<? extends R>> mapper) {
        return null;
    }

    default RefOPs<K, V, Ref> peek(Consumer<? super V> action) {
        return null;
    }

    // todo: fix
    @Deprecated
    default RefOPs<K, V, Ref> distinct() {
        return null;
    }

    // todo: fix
    @Deprecated
    default RefOPs<K, V, Ref> limit(long maxSize) {
        return null;
    }

    // todo: fix
    @Deprecated
    default RefOPs<K, V, Ref> skip(long skip) {
        return null;
    }

    default RefOPs<K, V, Ref> sorted() {
        return null;
    }

    default RefOPs<K, V, Ref> sorted(Comparator<? super V> comparator) {
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
