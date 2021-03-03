package org.comroid.mutatio.ref;

import org.comroid.abstr.AbstractList;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.adapter.StageAdapter;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.Pipeable;
import org.comroid.mutatio.span.Span;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@Experimental
public interface RefList<T> extends ValueCache<Void>, AbstractList<T>, Pipeable<T> {
    <R> ReferenceList<R> addStage(StageAdapter<T, R> stage);

    @Nullable Reference<T> getReference(int index);

    void close();

    boolean addReference(KeyedReference<@NotNull Integer, T> ref);

    ReferenceList<T> filter(Predicate<? super T> predicate);

    ReferenceList<T> yield(Predicate<? super T> predicate, Consumer<? super T> elseConsume);

    <R> ReferenceList<R> map(Function<? super T, ? extends R> mapper);

    <R> ReferenceList<R> flatMap(Class<R> target);

    <R> ReferenceList<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper);

    ReferenceList<T> peek(Consumer<? super T> action);

    @Deprecated
        // todo: fix
    ReferenceList<T> distinct();

    @Deprecated
        // todo: fix
    ReferenceList<T> limit(long maxSize);

    @Deprecated
        // todo: fix
    ReferenceList<T> skip(long skip);

    ReferenceList<T> sorted();

    ReferenceList<T> sorted(Comparator<? super T> comparator);

    @NotNull Reference<T> findFirst();

    @NotNull Reference<T> findAny();


    Optional<T> wrap(int index);

    T requireNonNull(int index) throws NullPointerException;

    T requireNonNull(int index, String message) throws NullPointerException;

    T requireNonNull(int index, Supplier<String> message) throws NullPointerException;

    @Deprecated
    Span<T> span();

    CompletableFuture<T> next();
}
