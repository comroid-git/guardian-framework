package org.comroid.mutatio.ref;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.api.ThrowingRunnable;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.*;
import org.comroid.mutatio.pipe.impl.SortedResultingPipe;
import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.span.Span;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ReferenceIndex<T> extends ValueCache.Abstract<Void> implements Pipeable<T>, ValueCache<Void>, Closeable {
    public static final int AUTOEMPTY_DISABLED = -1;
    protected final ReferenceIndex<?> refs;
    protected final int autoEmptyLimit;
    private final Collection<Pipe<?>> subs = new ArrayList<>();
    private final Reference.Advancer<?, T> adapter;
    private final Map<Integer, Reference<T>> accessors = new ConcurrentHashMap<>();
    private final List<Closeable> children = new ArrayList<>();

    public final Reference.Advancer<?, T> getAdapter() {
        return adapter;
    }

    public final Collection<? extends Closeable> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isSorted() {
        return false;
    }

    public ReferenceIndex(ReferenceIndex<?> old) {
        this(old, 100);
    }

    public ReferenceIndex(ReferenceIndex<?> old, int autoEmptyLimit) {
        this(old, StageAdapter.identity(), autoEmptyLimit);
    }

    public ReferenceIndex(ReferenceIndex<?> old, Reference.Advancer<?, T> adapter) {
        this(old, adapter, AUTOEMPTY_DISABLED);
    }

    protected ReferenceIndex(ReferenceIndex<?> old, Reference.Advancer<?, T> adapter, int autoEmptyLimit) {
        super(old);

        this.refs = old;
        this.adapter = adapter;
        this.autoEmptyLimit = autoEmptyLimit;
    }

    public static <T> ReferenceIndex<T> create() {
        return of(new ArrayList<>());
    }

    public static <T> ReferenceIndex<T> of(List<T> list) {
        Support.OfList<T> index = new Support.OfList<>();
        list.forEach(index::add);
        return index;
    }

    public static <T> ReferenceIndex<T> empty() {
        //noinspection unchecked
        return (ReferenceIndex<T>) Support.EMPTY;
    }

    @SafeVarargs
    public static <T> ReferenceIndex<T> of(T... values) {
        return of(Arrays.asList(values));
    }

    public static <T> ReferenceIndex<T> of(Collection<T> collection) {
        final ReferenceIndex<T> pipe = create();
        collection.forEach(pipe::add);

        return pipe;
    }

    public static <T> ReferenceIndex<T> ofStream(Stream<T> stream) {
        final ReferenceIndex<T> pipe = create();
        stream.iterator().forEachRemaining(pipe::add);
        return pipe;
    }

    public static <T> Collector<Pump<T>, List<Pump<T>>, Pipe<T>> resultingPipeCollector(Executor executor) {
        class ResultingPipeCollector implements Collector<Pump<T>, List<Pump<T>>, Pipe<T>> {
            private final Pump<T> yield = Pump.create(executor);
            private final Supplier<List<Pump<T>>> supplier = ArrayList::new;
            private final BiConsumer<List<Pump<T>>, Pump<T>> accumulator = List::add;
            private final BinaryOperator<List<Pump<T>>> combiner = (l, r) -> {
                l.addAll(r);
                return l;
            };
            private final Function<List<Pump<T>>, Pipe<T>> finisher = pipes -> {
                pipes.forEach(pump -> pump
                        .map(Reference::constant)
                        .map(ref -> ref.map(Object.class::cast))
                        .peek(yield));
                return yield;
            };

            @Override
            public Supplier<List<Pump<T>>> supplier() {
                return supplier;
            }

            @Override
            public BiConsumer<List<Pump<T>>, Pump<T>> accumulator() {
                return accumulator;
            }

            @Override
            public BinaryOperator<List<Pump<T>>> combiner() {
                return combiner;
            }

            @Override
            public Function<List<Pump<T>>, Pipe<T>> finisher() {
                return finisher;
            }

            @Override
            public Set<Characteristics> characteristics() {
                return Collections.singleton(Characteristics.IDENTITY_FINISH);
            }
        }

        return new ResultingPipeCollector();
    }

    public final void addChildren(Closeable child) {
        children.add(child);
    }

    public <R> ReferenceIndex<R> addStage(Reference.Advancer<T, R> stage) {
        return new ReferenceIndex.Support.WithStage<>(this, stage);
    }
/*
    public <X> ReferenceIndex<X, T> bi(Function<T, X> source) {
        return new KeyedPipe<>(this, BiStageAdapter.source(source), autoEmptyLimit);
    }
*/
    public int size() {
        return refs.size();
    }

    public boolean add(T item) {
        if (autoEmptyLimit != AUTOEMPTY_DISABLED
                && refs.size() >= autoEmptyLimit)
            refs.clear();

        return refs.add(Polyfill.uncheckedCast(item));
    }

    public void clear() {
        refs.clear();
    }

    public Stream<? extends Reference<T>> streamRefs() {
        // generate accessors
        refs.generateAccessors(accessors, Polyfill.uncheckedCast(getAdapter()), StageAdapter::advance);
        return accessors.values().stream();
    }

    @Override
    @Deprecated
    @Contract("-> this")
    public ReferenceIndex<T> pipe() {
        return this;
    }

    public Reference<T> getReference(int index) {
        return accessors.computeIfAbsent(index, key -> adapter.advance(prefabRef(refs.getReference(index))));
    }

    protected <R extends Reference<?>> R prefabRef(Reference<?> reference) {
        if (this instanceof BiPipe && !(reference instanceof KeyedReference)) {
            BiStageAdapter.Support.BiSource<T, ?> biSource = (BiStageAdapter.Support.BiSource<T, ?>) adapter;
            Object myKey = biSource.convertKey(reference.into(Polyfill::uncheckedCast));
            return Polyfill.uncheckedCast(new KeyedReference.Support.Base<>(Polyfill.uncheckedCast(myKey), (Reference<Object>) reference));
        } else return Polyfill.uncheckedCast(reference);
    }

    @Override
    public void close() throws IOException {
        for (Closeable child : getChildren())
            child.close();
    }

    public ReferenceIndex<T> subset() {
        return subset(0, size());
    }

    public ReferenceIndex<T> subset(int startIncl, int endExcl) {
        final ReferenceIndex<T> subset = create();

        for (int i = startIncl; i < endExcl; i++)
            subset.add(get(i));

        return subset;
    }

    public List<T> unwrap() {
        return span().unwrap();
    }

    @OverrideOnly
    public boolean addReference(Reference<T> in) {
        return false;
    }

    public ReferenceIndex<T> filter(Predicate<? super T> predicate) {
        return addStage(StageAdapter.filter(predicate));
    }

    public ReferenceIndex<T> yield(Predicate<? super T> predicate, Consumer<T> elseConsume) {
        return filter(it -> {
            if (predicate.test(it))
                return true;
            elseConsume.accept(it);
            return false;
        });
    }

    @Deprecated
    public <R> ReferenceIndex<R> map(Class<R> target) {
        return flatMap(target);
    }

    public <R> ReferenceIndex<R> map(Function<? super T, ? extends R> mapper) {
        return addStage(StageAdapter.map(mapper));
    }

    public <R> ReferenceIndex<R> flatMap(Class<R> target) {
        return filter(target::isInstance).map(target::cast);
    }

    public <R> ReferenceIndex<R> flatMap(Function<? super T, ? extends Rewrapper<? extends R>> mapper) {
        return addStage(StageAdapter.flatMap(mapper));
    }

    public ReferenceIndex<T> peek(Consumer<? super T> action) {
        return addStage(StageAdapter.peek(action));
    }

    public void forEach(Consumer<? super T> action) {
        addStage(StageAdapter.peek(action)).unwrap();
    }

    @Deprecated // todo: fix
    public ReferenceIndex<T> distinct() {
        return addStage(StageAdapter.distinct());
    }

    @Deprecated // todo: fix
    public ReferenceIndex<T> limit(long maxSize) {
        return addStage(StageAdapter.limit(maxSize));
    }

    @Deprecated // todo: fix
    public ReferenceIndex<T> skip(long skip) {
        return addStage(StageAdapter.skip(skip));
    }

    public ReferenceIndex<T> sorted() {
        return sorted(Polyfill.uncheckedCast(Comparator.naturalOrder()));
    }

    public ReferenceIndex<T> sorted(Comparator<? super T> comparator) {
        return new SortedResultingPipe<>(this, comparator);
    }

    @NotNull
    public Reference<T> findFirst() {
        return sorted().findAny();
    }

    @NotNull
    public Reference<T> findAny() {
        return Reference.conditional(() -> size() > 0, () -> get(0));
    }

    public boolean remove(T item) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("remove() is not supported by pipe");
    }

    public Stream<T> stream() {
        return unwrap().stream();
    }

    @Nullable
    public T get(int index) {
        Reference<T> ref = getReference(index);
        if (ref == null)
            return null;
        return ref.get();
    }

    public Optional<T> wrap(int index) {
        return getReference(index).wrap();
    }

    public @NotNull T requireNonNull(int index) throws NullPointerException {
        return getReference(index).requireNonNull();
    }

    public @NotNull T requireNonNull(int index, String message) throws NullPointerException {
        return getReference(index).requireNonNull(message);
    }

    @Internal
    @Experimental
    public <
            Out,
            InRef extends Reference<T>,
            OutRef extends Reference<Out>,
            Count,
            Adp extends StageAdapter<T, Out>
            > void generateAccessors(
            final Map<Integer, OutRef> accessors,
            final Adp adapter,
            final BiFunction<Adp, InRef, OutRef> refAccumulator
    ) {
        for (int i = 0; i < size(); i++) {
            //noinspection unchecked
            InRef inRef = (InRef) getReference(i);
            OutRef outRef = refAccumulator.apply(adapter, inRef);
            accessors.put(i, outRef);
        }
    }

    public Span<T> span() {
        return new Span<>(this, Span.DefaultModifyPolicy.SKIP_NULLS);
    }

    public CompletableFuture<T> next() {
        class OnceCompletingStage implements StageAdapter<T, T> {
            private final CompletableFuture<T> future = new CompletableFuture<>();

            @Override
            public Reference<T> advance(Reference<T> ref) {
                if (!ref.isNull() && !future.isDone())
                    future.complete(ref.get());
                return Reference.empty();
            }
        }

        final OnceCompletingStage stage = new OnceCompletingStage();
        final ReferenceIndex<T> resulting = addStage(stage);
        stage.future.thenRun(ThrowingRunnable.handling(resulting::close, null));

        return stage.future;
    }

    private static final class Support {
        public static final ReferenceIndex<?> EMPTY = ReferenceIndex.of(Collections.emptyList());

        private static class WithStage<T, R> extends ReferenceIndex<R> {
            private WithStage(ReferenceIndex<T> base, Reference.Advancer<T, R> stage) {
                super(base, stage);
            }
        }

        private static final class OfList<T> extends ReferenceIndex<T> {
            private final List<Reference<T>> list;

            private OfList() {
                this(new ArrayList<>());
            }

            private OfList(List<Reference<T>> list) {
                super(null);

                this.list = list;
            }

            @Override
            public boolean add(T item) {
                list.add(Reference.constant(item));
                updateCache();
                return true;
            }

            @Override
            public boolean addReference(Reference<T> in) {
                list.add(in);
                updateCache();
                return true;
            }

            @Override
            public boolean remove(T item) {
                if (list.removeIf(ref -> ref.contentEquals(item))) {
                    updateCache();
                    return true;
                }
                return false;
            }

            @Override
            public void clear() {
                list.clear();
                updateCache();
            }

            @Override
            public Stream<Reference<T>> streamRefs() {
                return list.stream();
            }

            @Override
            public Pipe<T> pipe() {
                return ReferenceIndex.of(list).flatMap(Function.identity());
            }

            @Override
            public Reference<T> getReference(final int index) {
                return list.get(index);
            }

            @Override
            public List<T> unwrap() {
                return Collections.unmodifiableList(list.stream()
                        .flatMap(Reference::stream)
                        .collect(Collectors.toList()));
            }

            @Override
            public int size() {
                return list.size();
            }
        }
    }
}
