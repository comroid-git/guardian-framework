package org.comroid.uniform.cache;

import org.comroid.api.ContextualProvider;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceList;
import org.comroid.mutatio.ref.ReferenceMap;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

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

        this.context = context;
    }

    protected abstract CacheReference<K, V> advanceIntoCacheRef(Reference<V> reference);
}
