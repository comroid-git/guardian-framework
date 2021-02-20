package org.comroid.mutatio.ref;

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
import java.util.function.Function;
import java.util.stream.Stream;

abstract class ReferenceAtlas<InK, K, In, V, InRef extends Reference<In>, OutRef extends Reference<V>>
        extends ValueCache.Abstract<Void, ReferenceAtlas<?, InK, ?, In, ?, InRef>> {
    public static abstract class ForList<In, T> extends ReferenceAtlas<@NotNull Integer, @NotNull Integer, In, T, Reference<In>, Reference<T>> {
        protected ForList(
                @Nullable ReferenceIndex<?, In> parent,
                @NotNull StageAdapter<In, T> advancer
        ) {
            super(parent, advancer, Function.identity(), Function.identity());
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
    }

    private final ReferenceConverter<InRef, OutRef> advancer;
    private final Map<K, OutRef> accessors = new ConcurrentHashMap<>();
    private final Function<InK, K> keyAdvancer;
    private final Function<K, InK> keyReverser;

    private ReferenceAtlas(
            @Nullable ReferenceAtlas<?, InK, ?, In, ?, InRef> parent,
            @NotNull ReferenceConverter<InRef, OutRef> advancer,
            @NotNull Function<InK, K> keyAdvancer,
            @NotNull Function<K, InK> keyReverser
    ) {
        super(parent);

        this.advancer = advancer;
        this.keyAdvancer = keyAdvancer;
        this.keyReverser = keyReverser;
    }

    protected abstract OutRef createEmptyRef(K key);

    public final int size() {
        return (int) streamKeys().count();
    }

    public final void clear() {
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
}
