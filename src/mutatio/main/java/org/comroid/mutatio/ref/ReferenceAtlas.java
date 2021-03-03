package org.comroid.mutatio.ref;

import org.comroid.abstr.AbstractList;
import org.comroid.abstr.AbstractMap;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.adapter.BiStageAdapter;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.adapter.StageAdapter;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.model.RefAtlas;
import org.comroid.mutatio.model.RefPipe;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class ReferenceAtlas<InK, K, In, V, InRef extends KeyedReference<InK, In>, OutRef extends KeyedReference<K, V>>
        extends ValueCache.Abstract<Void, RefAtlas<?, InK, ?, In, ?, InRef>>
        implements RefAtlas<InK, K, In, V, InRef, OutRef> {
    private final ReferenceStageAdapter<InK, K, In, V, InRef, OutRef> advancer;
    private final AtomicBoolean mutable;
    private final Map<K, OutRef> accessors;
    protected Comparator<OutRef> comparator;

    @Override
    public ReferenceStageAdapter<InK, K, In, V, InRef, OutRef> getAdvancer() {
        return advancer;
    }

    @Override
    public final boolean isMutable() {
        return mutable.get();
    }

    protected ReferenceAtlas(
            @Nullable RefAtlas<?, InK, ?, In, ?, InRef> parent,
            @NotNull ReferenceStageAdapter<InK, K, In, V, InRef, OutRef> advancer
    ) {
        this(parent, advancer, null);
    }

    protected ReferenceAtlas(
            @Nullable RefAtlas<?, InK, ?, In, ?, InRef> parent,
            @NotNull ReferenceStageAdapter<InK, K, In, V, InRef, OutRef> advancer,
            @Nullable Comparator<OutRef> comparator
    ) {
        super(parent);

        this.advancer = advancer;
        this.comparator = comparator;

        this.mutable = new AtomicBoolean(parent != null);
        this.accessors = new ConcurrentHashMap<>();
    }

    @Override
    public void close() {
        if (parent != null && !parent.removeDependent(this))
            throw new IllegalStateException("Could not remove from parent");
    }

    protected OutRef advanceReference(InRef inputRef) {
        if (getAdvancer() == null)
            throw new AbstractMethodError("Advancer not defined");
        return getAdvancer().advance(inputRef);
    }

    @Override
    public final boolean setMutable(boolean state) {
        return mutable.compareAndSet(!state, state);
    }

    protected abstract OutRef createEmptyRef(K key);

    /**
     * Reformats the key used for receiving parent using base access when creating a cascade reference.
     *
     * @param key Key to reformat
     * @return {@code null} if access was denied
     */
    @Internal
    protected @Nullable InK prefabBaseKey(InK key) {
        return key;
    }

    @Override
    public final int size() {
        return (int) streamKeys().count();
    }

    @Override
    public final boolean removeRef(K key) {
        validateMutability();
        InK revK = getAdvancer().revertKey(key).orElse(null);
        if (revK != null) {
            InRef pRef = parent == null ? null
                    : parent.getReference(revK, false);
            if (pRef != null)
                parent.removeRef(revK);
        }
        return accessors.remove(key) != null;
    }

    private void validateMutability() {
        if (isImmutable())
            throw new UnsupportedOperationException("Atlas is immutable");
    }

    @Override
    public final void clear() {
        validateMutability();
        /*
        if (parent != null)
            parent.clear();
         */
        accessors.clear();
    }

    @Override
    public final Stream<K> streamKeys() {
        return Stream.concat(
                parent == null ? Stream.empty()
                        : parent.streamKeys().map(getAdvancer()::advanceKey),
                accessors.keySet().stream()
        ).distinct();
    }

    @Override
    public final Stream<OutRef> streamRefs() {
        Stream<OutRef> stream = streamKeys().map(key -> getReference(key, true));
        if (comparator != null)
            stream = stream.sorted(comparator);
        return stream;
    }

    @Override
    public final Stream<V> streamValues() {
        return streamRefs().flatMap(Reference::stream);
    }

    @Override
    public final @Nullable InRef getInputReference(InK key, boolean createIfAbsent) {
        if (parent == null)
            return null;
        return parent.getReference(key, createIfAbsent);
    }

    @Override
    @Contract("!null, false -> _; !null, true -> !null; null, _ -> fail")
    public final OutRef getReference(K key, boolean createIfAbsent) {
        Objects.requireNonNull(key, "key");
        OutRef ref = accessors.get(key);
        if (ref != null | !createIfAbsent)
            return ref;
        validateMutability();
        InK fabK = getAdvancer().revertKey(key)
                .map(this::prefabBaseKey)
                .orElse(null);
        if (parent != null && fabK != null) {
            InRef inRef = getInputReference(fabK, true);
            if (inRef != null)
                ref = advanceReference(inRef);
        } else ref = createEmptyRef(key);
        if (accessors.containsKey(key))
            accessors.get(key).rebind(ref);
        else accessors.put(key, ref);
        return ref;
    }

    protected final boolean putAccessor(K key, OutRef ref) {
        validateMutability();
        return accessors.put(key, ref) != ref;
    }

    public static abstract class ForList<InV, V>
            extends ReferenceAtlas<@NotNull Integer, @NotNull Integer, InV, V, KeyedReference<@NotNull Integer, InV>, KeyedReference<@NotNull Integer, V>>
            implements AbstractList<V> {
        protected ForList(
                @Nullable ReferenceList<InV> parent,
                @NotNull StageAdapter<InV, V> advancer
        ) {
            super(parent, advancer);
        }

        protected ForList(
                @Nullable ReferenceList<InV> parent,
                @NotNull StageAdapter<InV, V> advancer,
                @Nullable Comparator<KeyedReference<@NotNull Integer, V>> comparator
        ) {
            super(parent, advancer, comparator);
        }

        @Override
        protected KeyedReference<@NotNull Integer, V> createEmptyRef(@NotNull Integer key) {
            return KeyedReference.createKey(key);
        }
    }

    public static abstract class ForMap<InK, InV, K, V>
            extends ReferenceAtlas<InK, K, InV, V, KeyedReference<InK, InV>, KeyedReference<K, V>>
            implements AbstractMap<K, V> {
        protected ForMap(
                @Nullable ReferenceMap<InK, InV> parent,
                @NotNull BiStageAdapter<InK, InV, K, V> advancer,
                @NotNull Function<K, InK> keyReverser
        ) {
            super(parent, advancer);
        }

        protected ForMap(
                @Nullable ReferenceMap<InK, InV> parent,
                @NotNull BiStageAdapter<InK, InV, K, V> advancer,
                @Nullable Comparator<KeyedReference<K, V>> comparator
        ) {
            super(parent, advancer, comparator);
        }

        @Override
        protected KeyedReference<K, V> createEmptyRef(K key) {
            return KeyedReference.createKey(key);
        }
    }
}
