package org.comroid.varbind;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.cache.BasicCache;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.cache.CacheReference;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.util.Map;
import java.util.function.BiFunction;

public abstract class DataContainerCache<K, V extends DataContainer<? super V>>
        extends BasicCache<K, V>
        implements Cache<K, V> {
    protected final VarBind<? super V, ?, ?, K> idBind;

    public DataContainerCache(
            ContextualProvider context,
            int largeThreshold,
            VarBind<? super V, ?, ?, K> idBind
    ) {
        super(context, largeThreshold);

        this.idBind = Polyfill.uncheckedCast(idBind);
    }

    public boolean add(V value) {
        final K key = value.requireNonNull(Polyfill.uncheckedCast(idBind));

        return put(key, value) != value;
    }

    public boolean remove(V value) {
        final K key = value.requireNonNull(Polyfill.uncheckedCast(idBind));

        return containsKey(key) && put(key, null) != value;
    }

    public final <T extends V> Reference<T> autoUpdate(BiFunction<ContextualProvider, UniObjectNode, T> resolver, UniObjectNode data) {
        final K key = idBind.getFrom(data);

        if (containsKey(key))
            //noinspection unchecked
            return getReference(key, false)
                    .peek(it -> it.updateFrom(data))
                    .map(it -> (T) it);
        else {
            T result = resolver.apply(this, data);
            KeyedReference<K, V> ref = getReference(key, true);
            ref.set(result);
            //noinspection unchecked
            return (Reference<T>) ref;
        }
    }
}
