package org.comroid.mutatio.ref;

import org.comroid.api.Polyfill;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.BiStageAdapter;
import org.comroid.mutatio.pipe.StageAdapter;

import java.util.Map;
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

    private ReferenceAtlas(
            ReferenceAtlas<?, InK, ?, In, ?, InRef> parent,
            ReferenceOverwriter<In, V, InRef, OutRef> advancer,
            Function<InK, K> keyAdvancer,
            Function<K, InK> keyReverser
    ) {
        super(parent);

        this.advancer = advancer;
        this.keyAdvancer = keyAdvancer;
        this.keyReverser = keyReverser;
    }

    public final Stream<K> streamKeys() {
        return Stream.concat(
                parent.streamKeys().map(keyAdvancer),
                accessors.keySet().stream()
        ).distinct();
    }

    public final Stream<V> streamValues() {
        return streamRefs().flatMap(Reference::stream);
    }

    public final Stream<OutRef> streamRefs() {
        return streamKeys().map(key -> getAccessor(key, true));
    }

    public final OutRef getAccessor(K key, boolean createIfAbsent) {
    }
}
