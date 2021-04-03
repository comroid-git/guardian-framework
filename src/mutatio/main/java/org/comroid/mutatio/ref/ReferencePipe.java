package org.comroid.mutatio.ref;

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

public class ReferencePipe<InK, InV, K, V>
        extends ReferenceAtlas<InK, K, InV, V>
        implements RefPipe<InK, InV, K, V> {
    @Nullable
    private final Executor stageExecutor;

    @Override
    @Nullable
    public final Executor getStageExecutor() {
        return stageExecutor;
    }

    public ReferencePipe(
    ) {
        this(null);
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
            final K key = getAdvancer().advanceKey(inK);
            final V value = getAdvancer().advanceValue(inK, inV);
            if (value != null)
                getDependents().stream()
                        .filter(ReferencePipe.class::isInstance)
                        .map(Polyfill::<ReferencePipe<K, V, ?, ?>>uncheckedCast)
                        .forEach(next -> next.accept(key, value));
        });
    }

    @Override
    protected KeyedReference<K, V> createEmptyRef(K key) {
        return KeyedReference.createKey(key);
    }

    @Override
    public void forEach(Consumer<? super V> action) {
        streamValues().forEach(action);
    }

    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {
        streamRefs().forEach(ref -> ref.consume(action));
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
