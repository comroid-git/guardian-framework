package org.comroid.mutatio.model;

import org.comroid.abstr.AbstractList;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.span.Span;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Experimental
public interface RefList<T> extends RefAtlas<@Nullable Integer, @NotNull Integer, Object, T, KeyedReference<@Nullable Integer, Object>, KeyedReference<@NotNull Integer, T>>, ValueCache<Void>, AbstractList<T> {
    @Nullable Reference<T> getReference(int index);

    void close();

    boolean addReference(KeyedReference<@NotNull Integer, T> ref);


    Optional<T> wrap(int index);

    T requireNonNull(int index) throws NullPointerException;

    T requireNonNull(int index, String message) throws NullPointerException;

    T requireNonNull(int index, Supplier<String> message) throws NullPointerException;

    @Deprecated
    Span<T> span();

    CompletableFuture<T> next();
}
