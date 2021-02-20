package org.comroid.mutatio.ref;

import org.comroid.mutatio.MutableState;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.BiStageAdapter;
import org.comroid.mutatio.pipe.ReferenceConverter;
import org.comroid.mutatio.pipe.StageAdapter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class ReferenceAtlas<InK, K, In, V, InRef extends Reference<In>, OutRef extends Reference<V>>
        extends ValueCache.Abstract<Void, ReferenceAtlas<?, InK, ?, In, ?, InRef>>
        implements MutableState {
    private final AtomicBoolean mutable;
    private final Map<K, OutRef> accessors;
    private final ReferenceConverter<InRef, OutRef> advancer;
    private final Function<InK, K> keyAdvancer;
    private final Function<K, InK> keyReverser;

    @Override
    public final boolean isMutable() {
        return mutable.get();
    }

    protected ReferenceAtlas(
            @Nullable ReferenceAtlas<?, InK, ?, In, ?, InRef> parent,
            @NotNull ReferenceConverter<InRef, OutRef> advancer,
            @NotNull Function<InK, K> keyAdvancer,
            @Nullable Function<K, InK> keyReverser
    ) {
        super(parent);

        this.advancer = advancer;
        this.keyAdvancer = keyAdvancer;
        this.keyReverser = keyReverser;

        this.mutable = new AtomicBoolean(parent != null);
        this.accessors = new ConcurrentHashMap<>();
    }

    @Override
    public final boolean setMutable(boolean state) {
        return mutable.compareAndSet(!state, state);
    }

    protected abstract OutRef createEmptyRef(K key);

    public final int size() {
        return (int) streamKeys().count();
    }

    public final boolean removeRef(K key) {
        validateMutability();
        InK revK = keyReverser == null ? null : keyReverser.apply(key);
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

    public final void clear() {
        validateMutability();
        if (parent != null)
            parent.clear();
        accessors.clear();
    }

    public final Stream<K> streamKeys() {
        return Stream.concat(
                parent == null ? Stream.empty()
                        : parent.streamKeys().map(keyAdvancer),
                accessors.keySet().stream()
        ).distinct();
    }

    public final Stream<V> streamValues() {
        return streamRefs().flatMap(Reference::stream);
    }

    public final Stream<OutRef> streamRefs() {
        return streamKeys().map(key -> getReference(key, true));
    }

    @Contract("!null, false -> _; !null, true -> !null; null, _ -> fail")
    public final OutRef getReference(K key, boolean createIfAbsent) {
        Objects.requireNonNull(key, "key");
        OutRef ref = accessors.get(key);
        if (ref != null | !createIfAbsent)
            return ref;
        validateMutability();
        InK revK = keyReverser == null ? null : keyReverser.apply(key);
        if (parent != null && revK != null) {
            InRef inRef = parent.getReference(revK, false);
            if (inRef != null)
                ref = advancer.advance(inRef);
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

    public static abstract class ForList<In, T> extends ReferenceAtlas<@NotNull Integer, @NotNull Integer, In, T, Reference<In>, Reference<T>> {
        protected ForList(
                @Nullable ReferenceIndex<?, In> parent,
                @NotNull StageAdapter<In, T> advancer
        ) {
            super(parent, advancer, Function.identity(), Function.identity());
        }

        @Override
        protected final Reference<T> createEmptyRef(@NotNull Integer key) {
            return KeyedReference.createKey(key);
        }
    }

    public static abstract class ForMap<InK, InV, K, V> extends ReferenceAtlas<InK, K, InV, V, KeyedReference<InK, InV>, KeyedReference<K, V>> {
        protected ForMap(
                @Nullable ReferenceMap<?, ?, InK, InV> parent,
                @NotNull BiStageAdapter<InK, InV, K, V> advancer,
                @NotNull Function<K, InK> keyReverser
        ) {
            super(parent, advancer, advancer::advanceKey, keyReverser);
        }

        @Override
        protected final KeyedReference<K, V> createEmptyRef(K key) {
            return KeyedReference.createKey(key);
        }
    }
}
