package org.comroid.mutatio.pipe;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class BasicPipe<O, T> implements Pipe<T> {
    public static final int AUTOEMPTY_DISABLED = -1;
    protected final ReferenceIndex<O> refs;
    private final Collection<Pipe<?>> subs = new ArrayList<>();
    private final StageAdapter<O, T> adapter;
    private final int autoEmptyLimit;
    private final Map<Integer, Reference<T>> accessors = new ConcurrentHashMap<>();
    private final List<AutoCloseable> children = new ArrayList<>();

    @Override
    public StageAdapter<O, T> getAdapter() {
        return adapter;
    }

    public final Collection<? extends AutoCloseable> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public BasicPipe(ReferenceIndex<O> old) {
        this(old, 100);
    }

    public BasicPipe(ReferenceIndex<O> old, int autoEmptyLimit) {
        //noinspection unchecked
        this(old, StageAdapter.map(it -> (T) it), autoEmptyLimit);
    }

    public BasicPipe(ReferenceIndex<O> old, StageAdapter<O, T> adapter) {
        this(old, adapter, AUTOEMPTY_DISABLED);
    }

    public BasicPipe(ReferenceIndex<O> old, StageAdapter<O, T> adapter, int autoEmptyLimit) {
        this.refs = old;
        this.adapter = adapter;
        this.autoEmptyLimit = autoEmptyLimit;
    }

    public final void addChildren(AutoCloseable child) {
        children.add(child);
    }

    @Override
    public <R> Pipe<R> addStage(StageAdapter<T, R> stage) {
        return new BasicPipe<>(this, stage);
    }

    @Override
    public <X> BiPipe<T, X, T, X> bi(Function<T, X> source) {
        return new BiPipe<>(this, source);
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
    public void accept(Reference<Object> other) {
        refs.add(other.into(Polyfill::<O>uncheckedCast));
    }

    @Override
    public Pipe<T> pipe() {
        return new BasicPipe<>(refs);
    }

    @Override
    public Reference<T> getReference(int index) {
        return accessors.computeIfAbsent(index, key -> adapter.advance(refs.getReference(index)));
    }

    @Override
    public void close() throws Exception {
        for (AutoCloseable child : getChildren())
            child.close();
    }
}
