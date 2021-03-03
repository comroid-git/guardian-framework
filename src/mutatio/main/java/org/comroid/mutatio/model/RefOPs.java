package org.comroid.mutatio.model;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public interface RefOPs<K, V, Ref extends Reference<V>> extends Pipeable<V> {
    <X, Y, OutRef extends KeyedReference<X, Y>> RefOPs<X, Y, OutRef> addStage(ReferenceStageAdapter<K, X, V, Y, Ref, OutRef> adapter);

    RefOPs<K, V, Ref> filter(Predicate<? super V> predicate);

    RefOPs<K, V, Ref> yield(Predicate<? super V> predicate, Consumer<? super V> elseConsume);

    <R> RefOPs<K, R, ? extends Reference<R>> map(Function<? super V, ? extends R> mapper);

    <R> RefOPs<K, R, ? extends Reference<R>> flatMap(Class<R> target);

    <R> RefOPs<K, R, ? extends Reference<R>> flatMap(Function<? super V, ? extends Rewrapper<? extends R>> mapper);

    <R> RefOPs<K, R, ? extends Reference<R>> flatMapOptional(Function<? super V, ? extends Optional<? extends R>> mapper);

    RefOPs<K, V, Ref> peek(Consumer<? super V> action);

    // todo: fix
    @Deprecated
    RefOPs<K, V, Ref> distinct();

    // todo: fix
    @Deprecated
    RefOPs<K, V, Ref> limit(long maxSize);

    // todo: fix
    @Deprecated
    RefOPs<K, V, Ref> skip(long skip);

    RefOPs<K, V, Ref> sorted();

    RefOPs<K, V, Ref> sorted(Comparator<? super V> comparator);

    @NotNull Reference<V> findFirst();

    @NotNull Reference<V> findAny();
}
