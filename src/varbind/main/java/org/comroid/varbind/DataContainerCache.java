package org.comroid.varbind;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceList;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.cache.BasicCache;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.Experimental;

import java.sql.*;
import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

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
        this.idColumn = idColumn == null ? idBind.getFieldName() : idColumn;
    }

    @Experimental
    public final int updateFrom(ResultSet results) throws SQLException {
        return updateFrom(results, idColumn);
    }

    @Experimental
    public final int updateFrom(final ResultSet results, String idColumn) throws SQLException {
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

    private UniObjectNode copyBindsFromResultSet(ContextualProvider ctx, GroupBind<? super V> group, ResultSet results) throws SQLException {
        UniObjectNode obj = ctx.getFromContext(SerializationAdapter.class)
                .orElseThrow(() -> new NoSuchElementException("Missing SerializationAdapter"))
                .createObjectNode();

        for (VarBind<? super V, ?, ?, ?> bind : group.streamAllChildren().collect(Collectors.toList())) {
            // fixme Handle arrays & lists & uninodes
            Object object = results.getObject(bind.getFieldName(), bind.getHeldType().getTargetClass());
            obj.put(bind.getFieldName(), object);
        }

        return obj;
    }

    @Experimental
    public final int updateInto(ResultSet results) throws SQLException {
        return updateInto(results, idColumn);
    }

    @Experimental
    public final int updateInto(final ResultSet results, String idColumn) throws SQLException {
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

    @Experimental
    public final Reference<V> autoUpdate(UniObjectNode data) {
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

    @Experimental
    public final int reloadData(Connection connection, String table) throws SQLException {
        try (
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + ";");
                ResultSet results = statement.executeQuery()
        ) {
            int operations = updateFrom(results);
            getLogger().trace("Loaded data from table {} in {} operations using connection {}", table, operations, connection);
            return operations;
        } catch (SQLFeatureNotSupportedException fnse) {
            getLogger().debug("Could not load data from table {} because the driver does not support this method", table, fnse);
        }
    }

    @Experimental
    public final int saveData(Connection connection, String table) throws SQLException {
        try (
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM " + table + ";");
                ResultSet results = statement.executeQuery()
        ) {
            int operations = updateInto(results);
            getLogger().trace("Saved data into table {} in {} operations using connection {}", table, operations, connection);
            return operations;
        } catch (SQLFeatureNotSupportedException fnse) {
            getLogger().debug("Could not save data into table {} because the driver does not support this method", table, fnse);
        }
    }
}
