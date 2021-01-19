package org.comroid.mutatio.pipe.impl;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.BiPipe;
import org.comroid.mutatio.pipe.BiStageAdapter;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.pipe.StageAdapter;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class BasicPipe<O, T> extends ValueCache.Underlying<Void> implements Pipe<T> {
    public static final int AUTOEMPTY_DISABLED = -1;
    protected final ReferenceIndex<O> refs;
    private final Collection<Pipe<?>> subs = new ArrayList<>();
    private final StageAdapter<O, T, ? extends Reference<O>, ? extends Reference<T>> adapter;
    protected final int autoEmptyLimit;
    private final Map<Integer, Reference<T>> accessors = new ConcurrentHashMap<>();
    private final List<Closeable> children = new ArrayList<>();

    @Override
    public StageAdapter<?, T, Reference<?>, Reference<T>> getAdapter() {
        return Polyfill.uncheckedCast(adapter);
    }

    public final Collection<? extends Closeable> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public BasicPipe(ReferenceIndex<O> old) {
        this(old, 100);
    }

    public BasicPipe(ReferenceIndex<O> old, int autoEmptyLimit) {
        //noinspection unchecked
        this(old, StageAdapter.map(it -> (T) it), autoEmptyLimit);
    }

    public BasicPipe(ReferenceIndex<O> old, StageAdapter<O, T, Reference<O>, Reference<T>> adapter) {
        this(old, adapter, AUTOEMPTY_DISABLED);
    }

    protected BasicPipe(ReferenceIndex<O> old, StageAdapter<O, T, ? extends Reference<O>, ? extends Reference<T>> adapter, int autoEmptyLimit) {
        super(old);

        this.refs = old;
        this.adapter = adapter;
        this.autoEmptyLimit = autoEmptyLimit;
    }

    public final void addChildren(Closeable child) {
        children.add(child);
    }

    @Override
    public <R> Pipe<R> addStage(StageAdapter<T, R, Reference<T>, Reference<R>> stage) {
        return new BasicPipe<>(this, stage);
    }

    @Override
    public <X> BiPipe<X, T> bi(Function<T, X> source) {
        return new KeyedPipe<>(this, BiStageAdapter.source(source), autoEmptyLimit);
    }

    @Override
    public int size() {
        return refs.size();
    }

    @Override
    public boolean add(T item) {
        if (autoEmptyLimit != AUTOEMPTY_DISABLED
                && refs.size() >= autoEmptyLimit)
            refs.clear();

        return refs.add(Polyfill.uncheckedCast(item));
    }

    @Override
    public void clear() {
        refs.clear();
    }

    @Override
    public Stream<? extends Reference<T>> streamRefs() {
        // generate accessors
        refs.generateAccessors(accessors, Polyfill.uncheckedCast(getAdapter()), StageAdapter::advance);
        return accessors.values().stream();
    }

    @Override
    public Pipe<T> pipe() {
        return new BasicPipe<>(refs);
    }

    @Override
    public Reference<T> getReference(int index) {
        return accessors.computeIfAbsent(index, key -> adapter.advance(prefabRef(refs.getReference(index))));
    }

    protected <R extends Reference<O>> R prefabRef(Reference<O> reference) {
        if (this instanceof BiPipe && !(reference instanceof KeyedReference)) {
            BiStageAdapter.Support.BiSource<T, ?> biSource = (BiStageAdapter.Support.BiSource<T, ?>) adapter;
            Object myKey = biSource.convertKey(reference.into(Polyfill::uncheckedCast));
            return Polyfill.uncheckedCast(new KeyedReference.Support.Base<O, O>(Polyfill.uncheckedCast(myKey), reference));
        } else return Polyfill.uncheckedCast(reference);
    }

    @Override
    public void close() throws IOException {
        for (Closeable child : getChildren())
            child.close();
    }
}
