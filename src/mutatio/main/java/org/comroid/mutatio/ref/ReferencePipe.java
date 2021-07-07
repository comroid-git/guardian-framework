package org.comroid.mutatio.ref;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.abstr.AbstractList;
import org.comroid.abstr.AbstractMap;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.adapter.BiStageAdapter;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.adapter.StageAdapter;
import org.comroid.mutatio.model.RefAtlas;
import org.comroid.mutatio.model.RefPipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ReferencePipe<InK, InV, K, V>
        extends ReferenceAtlas<InK, K, InV, V>
        implements RefPipe<InK, InV, K, V> {
    private static final Logger logger = LogManager.getLogger();
    @Nullable
    private final Executor stageExecutor;

    @Override
    @Nullable
    public final Executor getStageExecutor() {
        return stageExecutor;
    }

    /**
     * @deprecated Use {@link ReferencePipe#ReferencePipe(Executor)}
     */
    @Deprecated
    public ReferencePipe(
    ) {
        this((RefAtlas<?, K, ?, V>) null);
    }

    public ReferencePipe(
            @Nullable RefAtlas<?, K, ?, V> parent
    ) {
        this(Polyfill.uncheckedCast(parent), Polyfill.uncheckedCast(BiStageAdapter.identity()));
    }

    public ReferencePipe(
            @Nullable RefAtlas<?, InK, ?, InV> parent,
            @NotNull ReferenceStageAdapter<InK, K, InV, V, KeyedReference<InK, InV>, KeyedReference<K, V>> advancer
    ) {
        this(parent, advancer, getExecutorFromAtlas(parent));
    }

    public ReferencePipe(
            @Nullable Executor stageExecutor
    ) {
        this(null, Polyfill.uncheckedCast(BiStageAdapter.identity()), stageExecutor);
    }

    public ReferencePipe(
            @Nullable RefAtlas<?, InK, ?, InV> parent,
            @NotNull ReferenceStageAdapter<InK, K, InV, V, KeyedReference<InK, InV>, KeyedReference<K, V>> advancer,
            @Nullable Executor stageExecutor
    ) {
        super(parent, advancer);

        this.stageExecutor = stageExecutor;
    }

    public ReferencePipe(
            @Nullable RefAtlas<?, InK, ?, InV> parent,
            @NotNull ReferenceStageAdapter<InK, K, InV, V, KeyedReference<InK, InV>, KeyedReference<K, V>> advancer,
            @Nullable Comparator<KeyedReference<K, V>> comparator
    ) {
        this(parent, advancer, comparator, getExecutorFromAtlas(parent));
    }

    public ReferencePipe(
            @Nullable RefAtlas<?, InK, ?, InV> parent,
            @NotNull ReferenceStageAdapter<InK, K, InV, V, KeyedReference<InK, InV>, KeyedReference<K, V>> advancer,
            @Nullable Comparator<KeyedReference<K, V>> comparator,
            @Nullable Executor stageExecutor
    ) {
        super(parent, advancer, comparator);

        this.stageExecutor = stageExecutor;
    }

    @Nullable
    private static Executor getExecutorFromAtlas(RefAtlas<?, ?, ?, ?> parent) {
        if (parent instanceof RefPipe)
            return ((RefPipe) parent).getStageExecutor();
        return null;
    }

    @Override
    public final void accept(InK inK, InV inV) {
        callDependentStages(stageExecutor == null ? Runnable::run : stageExecutor, inK, inV);
    }

    @Override
    public final void callDependentStages(Executor executor, InK inK, InV inV) {
        executor.execute(() -> {
            ReferenceStageAdapter<InK, K, InV, V, KeyedReference<InK, InV>, KeyedReference<K, V>> advancer = getAdvancer();
            final K key = advancer.advanceKey(inK);
            final V value = advancer.advanceValue(inK, inV);
            if (advancer.isFiltering() && inV != null && value == null)
                return;
            getDependents().stream()
                    .filter(ReferencePipe.class::isInstance)
                    .map(Polyfill::<ReferencePipe<K, V, ?, ?>>uncheckedCast)
                    .forEach(next -> {
                        try {
                            next.accept(key, value);
                        } catch (Throwable t) {
                            logger.error("An error occurred during forwarding to pipe " + next, t);
                        }
                    });
        });
    }

    @Override
    protected KeyedReference<K, V> createEmptyRef(K key) {
        return KeyedReference.createKey(key);
    }

    public static abstract class ForList<InV, V>
            extends ReferencePipe<@NotNull Integer, InV, @NotNull Integer, V>
            implements AbstractList<V> {
        protected ForList(
                @Nullable ReferenceList<InV> parent,
                @NotNull StageAdapter<InV, V> advancer
        ) {
            super(parent, advancer, getExecutorFromAtlas(parent));
        }

        public ForList(
                @Nullable ReferenceList<InV> parent,
                @NotNull StageAdapter<InV, V> advancer,
                @Nullable Executor stageExecutor
        ) {
            super(parent, advancer, stageExecutor);
        }

        protected ForList(
                @Nullable ReferenceList<InV> parent,
                @NotNull StageAdapter<InV, V> advancer,
                @Nullable Comparator<KeyedReference<@NotNull Integer, V>> comparator
        ) {
            super(parent, advancer, comparator, getExecutorFromAtlas(parent));
        }

        public ForList(
                @Nullable ReferenceList<InV> parent,
                @NotNull StageAdapter<InV, V> advancer,
                @Nullable Comparator<KeyedReference<@NotNull Integer, V>> comparator,
                @Nullable Executor stageExecutor
        ) {
            super(parent, advancer, comparator, stageExecutor);
        }

        @Override
        public Stream<V> stream() {
            return super.stream();
        }
    }

    public static abstract class ForMap<InK, InV, K, V>
            extends ReferencePipe<InK, InV, K, V>
            implements AbstractMap<K, V> {
        protected ForMap(
                @Nullable ReferenceMap<InK, InV> parent,
                @NotNull BiStageAdapter<InK, InV, K, V> advancer
        ) {
            super(parent, advancer, getExecutorFromAtlas(parent));
        }

        public ForMap(
                @Nullable ReferenceMap<InK, InV> parent,
                @NotNull BiStageAdapter<InK, InV, K, V> advancer,
                @Nullable Executor stageExecutor
        ) {
            super(parent, advancer, stageExecutor);
        }

        protected ForMap(
                @Nullable ReferenceMap<InK, InV> parent,
                @NotNull BiStageAdapter<InK, InV, K, V> advancer,
                @Nullable Comparator<KeyedReference<K, V>> comparator
        ) {
            super(parent, advancer, comparator, getExecutorFromAtlas(parent));
        }

        public ForMap(
                @Nullable ReferenceMap<InK, InV> parent,
                @NotNull BiStageAdapter<InK, InV, K, V> advancer,
                @Nullable Comparator<KeyedReference<K, V>> comparator,
                @Nullable Executor stageExecutor
        ) {
            super(parent, advancer, comparator, stageExecutor);
        }
    }
}
