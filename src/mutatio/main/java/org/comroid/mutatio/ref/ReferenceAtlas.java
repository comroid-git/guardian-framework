package org.comroid.mutatio.ref;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.BiStageAdapter;
import org.comroid.mutatio.pipe.StageAdapter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.comroid.api.Polyfill.uncheckedCast;

public abstract class ReferenceAtlas<InK, K, In, V, InRef extends Reference<In>, OutRef extends Reference<V>>
        extends ValueCache.Abstract<Void, ReferenceAtlas<?, InK, ?, In, ?, InRef>> {
    private final ReferenceOverwriter<In, V, InRef, OutRef> advancer;
    private final Map<K, OutRef> accessors = new ConcurrentHashMap<>();
    private final Function<InK, K> keyAdvancer;
    private final Function<K, InK> keyReverser;

    protected ReferenceAtlas(
            ReferenceIndex<In> parent,
            StageAdapter<In, V> adapter
    ) {
        this(uncheckedCast(parent), uncheckedCast(adapter), Polyfill::uncheckedCast, Polyfill::uncheckedCast);
    }

    protected ReferenceAtlas(
            ReferenceMap<InK, In> parent,
            BiStageAdapter<InK, In, K, V> adapter,
            Function<K, InK> keyReverser
    ) {
        this(uncheckedCast(parent), uncheckedCast(adapter), adapter::advanceKey, keyReverser);
    }

    protected ReferenceAtlas(
            @Nullable ReferenceAtlas<?, InK, ?, In, ?, InRef> parent,
            ReferenceOverwriter<In, V, InRef, OutRef> advancer,
            Function<InK, K> keyAdvancer,
            Function<K, InK> keyReverser
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
        InK revK = keyReverser.apply(key);
        if (parent != null) {
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
