package org.comroid.varbind;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceList;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.cache.BasicCache;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.ApiStatus;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;

public class DataContainerCache<K, V extends DataContainer<? super V>>
        extends BasicCache<K, V>
        implements Cache<K, V> {
    protected final VarBind<? super V, K, ?, K> idBind;
    protected final String idColumn;

    public DataContainerCache(
            ContextualProvider context,
            int largeThreshold,
            VarBind<? super V, K, ?, K> idBind
    ) {
        this(context, largeThreshold, idBind, null);
    }

    public DataContainerCache(
            ContextualProvider context,
            int largeThreshold,
            VarBind<? super V, K, ?, K> idBind,
            String idColumn
    ) {
        super(context, largeThreshold);

        this.idBind = Polyfill.uncheckedCast(idBind);
        this.idColumn = idColumn;
    }

    public int updateFrom(ResultSet results) throws SQLException {
        return updateFrom(results, idColumn);
    }

    public int updateFrom(final ResultSet results, String idColumn) throws SQLException {
        int c = 0;
        idColumn = Polyfill.notnullOr(idColumn, this.idColumn);

        while (results.next()) {
            final K id = results.getObject(idColumn, idBind.getHeldType().getTargetClass());
            V container;
            if (!containsKey(id) || (container = get(id)) == null) {
                // need to create object
                //noinspection unchecked
                Rewrapper<? extends BiFunction<ContextualProvider, UniNode, V>> resolver =
                        (Rewrapper<? extends BiFunction<ContextualProvider, UniNode, V>>) idBind.getGroup().getResolver();
                if (resolver.isNull()) {
                    getLogger().debug("Skipped updating data for ID {} because the corresponding object was not found in cache", id);
                    continue;
                }
                UniObjectNode data = copyBindsFromResultSet(this, idBind.getGroup(), results);
                container = resolver.into(it -> it.apply(this, data));
                put(id, container);
                continue;
            }

            //noinspection unchecked
            for (VarBind<?, Object, ?, Object> bind : container.keySet()) {
                final String updateKey = bind.getFieldName();
                //noinspection rawtypes
                KeyedReference<String, ReferenceList> ref = container.getInputReference(updateKey, true);

                if (bind.isListing()) {
                    // expect array
                    // fixme WILL fail with primitive types
                    final Object[] array = (Object[]) results.getArray(updateKey).getArray();
                    ref.compute(refs -> {
                        if (refs == null)
                            return Span.immutable(array);
                        refs.clear();
                        //noinspection unchecked
                        refs.addAll(Arrays.asList(array));
                        return refs;
                    });
                    c++;
                } else {
                    // expect object
                    final Object object = results.getObject(updateKey, bind.getHeldType().getTargetClass());
                    ref.compute(refs -> {
                        if (refs == null)
                            return Span.immutable(object);
                        refs.clear();
                        //noinspection unchecked
                        refs.add(object);
                        return refs;
                    });
                    c++;
                }
            }
        }
        return c;
    }

    private UniObjectNode copyBindsFromResultSet(ContextualProvider ctx, GroupBind<? super V> group, ResultSet results) {
        return null;
    }

    public int updateInto(ResultSet results) throws SQLException {
        return updateInto(results, idColumn);
    }

    public int updateInto(final ResultSet results, String idColumn) throws SQLException {
        int c = 0;
        idColumn = Polyfill.notnullOr(idColumn, this.idColumn);

        for (Map.Entry<K, V> entry : entrySet()) {
            final K id = entry.getKey();
            final V container = entry.getValue();

            //noinspection unchecked
            for (VarBind<?, Object, ?, Object> bind : container.keySet()) {
                final String sendKey = bind.getFieldName();
                //noinspection rawtypes
                KeyedReference<String, ReferenceList> ref = container.getInputReference(sendKey, true);

                if (bind.isListing()) {
                    // expect array
                    // todo
                    getLogger().debug("Skipped updating data for ID {} because the cache currently cannot input arrays", id);
                } else {
                    // expect object
                    final Object object = ref.ifPresentMap(refs -> refs.get(0));
                    results.updateObject(sendKey, object);
                    c++;
                }
            }
        }
        return c;
    }

    public boolean add(V value) {
        final K key = value.requireNonNull(Polyfill.uncheckedCast(idBind));

        return put(key, value) != value;
    }

    public boolean remove(V value) {
        final K key = value.requireNonNull(Polyfill.uncheckedCast(idBind));

        return containsKey(key) && put(key, null) != value;
    }

    @ApiStatus.Experimental
    public final <T extends V> Reference<T> autoUpdate(UniObjectNode data) {
        return autoUpdate(Polyfill.uncheckedCast(idBind.getGroup().getResolver().orElseThrow()), data);
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
