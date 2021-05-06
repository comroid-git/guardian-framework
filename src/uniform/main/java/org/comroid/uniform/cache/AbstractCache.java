package org.comroid.uniform.cache;

import org.comroid.api.ContextualProvider;
import org.comroid.mutatio.ref.ReferenceMap;

public abstract class AbstractCache<K, V>
        extends ReferenceMap<K, V>
        implements Cache<K, V> {
    private final ContextualProvider context;

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    protected AbstractCache(ContextualProvider context) {
        super(true);
        context.addToContext(this);

        this.context = context;
    }

    @Override
    protected abstract CacheReference<K, V> createEmptyRef(K key);
}
