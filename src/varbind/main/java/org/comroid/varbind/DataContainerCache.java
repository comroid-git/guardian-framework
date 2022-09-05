package org.comroid.varbind;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.cache.BasicCache;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

import java.sql.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;

public class DataContainerCache<K, V extends DataContainer<? super V>>
        extends BasicCache<K, V>
        implements Cache<K, V> {
    private static final Logger logger = LogManager.getLogger();
    protected final VarBind<? super V, ?, ?, K> idBind;

    public VarBind<? super V, ?, ?, K> getIdBind() {
        return idBind;
    }

    public DataContainerCache(
            VarBind<? super V, ?, ?, K> idBind
    ) {
        this(100, idBind);
    }

    public DataContainerCache(
            int largeThreshold,
            VarBind<? super V, ?, ?, K> idBind
    ) {
        this(ContextualProvider.getRoot(), largeThreshold, idBind);
    }

    public DataContainerCache(
            ContextualProvider context,
            int largeThreshold,
            VarBind<? super V, ?, ?, K> idBind
    ) {
        super(context, largeThreshold);

        this.idBind = Polyfill.uncheckedCast(idBind);
    }

    @Experimental
    @SuppressWarnings("rawtypes")
    public final int updateFrom(ResultSet results) throws SQLException {
        int c = 0;

        while (results.next()) {
            GroupBind<? super V> group = findGroup_rec(idBind.getGroup(), results.getString("group"));
            if (group == null) {
                logger.warn("Skipping entry " + idBind.getFrom(results) + " because group was not found: " + results.getString("group"));
                continue;
            }
            Iterator<? extends VarBind<? super V, ?, ?, ?>> binds = group.streamAllChildren().iterator();
            K id = idBind.getFrom(results);
            V obj = get(id);

            if (obj == null) {
                // need to create object
                BiFunction<ContextualProvider, UniNode, ? super V> ctor = group.getResolver().get();
                if (ctor == null) {
                    logger.warn("Skipping entry " + idBind.getFrom(results) + " because no constructor was found for group " + results.getString("group"));
                    continue;
                }
                SerializationAdapter serializer = getFromContext(SerializationAdapter.class).assertion("Unable to find serializer in context");
                UniObjectNode node = serializer.createObjectNode();

                while (binds.hasNext()) {
                    VarBind<? super V, ?, ?, ?> bind = binds.next();
                    if (bind.ignoreInDB())
                        continue;
                    node.put(bind.getFieldName(), bind.getFrom(results));
                    c++;
                }

                obj = Polyfill.uncheckedCast(ctor.apply(this, node));
                add(obj);
            } else {
                while (binds.hasNext()) {
                    VarBind<? super V, ?, ?, ?> bind = binds.next();
                    if (bind.ignoreInDB())
                        continue;
                    obj.put(bind.getFieldName(), bind.getFrom(results));
                    c++;
                }
            }
        }

        return c;
    }

    private @Nullable GroupBind<? super V> findGroup_rec(GroupBind<?> base, String name) {
        GroupBind<? super V> result = null;
        for (GroupBind<?> subgroup : base.getSubgroups()) {
            result = findGroup_rec(subgroup, name);
            if (result == null && subgroup.getName().equals(name))
                result = Polyfill.uncheckedCast(subgroup);
            if (result != null)
                break;
        }
        return result;
    }

    @Experimental
    public final int updateInto(final ResultSet results) throws SQLException {
        int c = 0;
        Set<K> remaining = new HashSet<>(keySet());

        // update existing
        while (results.next()) {
            K id = idBind.getFrom(results);
            V obj = get(id);

            if (obj == null) {
                logger.warn("Unable to find object with id {}. Skipping.", id);
                continue;
            }

            c += exportInto(results, obj);
            results.updateRow();
            remaining.remove(id);
        }
        // insert nonexistent
        for (K id : remaining) {
            V obj = get(id);

            if (obj == null) {
                logger.warn("Unable to find object with id {}. Skipping.", id);
                continue;
            }
            results.moveToInsertRow();
            c += exportInto(results, obj);
            results.insertRow();
        }
        return c;
    }

    private int exportInto(ResultSet results, V obj) throws SQLException {
        int c = 0;
        //noinspection unchecked
        for (VarBind<?, Object, ?, Object> bind : obj.keySet()) {
            if (bind.ignoreInDB())
                continue;
            bind.putInto(results, obj);
            c++;
        }
        results.updateString("group", obj.getRootBind().getName());
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
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM `" + table + "`;");
                ResultSet results = statement.executeQuery()
        ) {
            int operations = updateFrom(results);
            logger.trace("Loaded data from table {} in {} operations using connection {}", table, operations, connection);
            return operations;
        } catch (SQLFeatureNotSupportedException fnse) {
            logger.debug("Could not load data from table {} because the driver does not support this method", table, fnse);
            return -1;
        }
    }

    @Experimental
    public final int saveData(Connection connection, String table) throws SQLException {
        try (
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM `" + table + "`;", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
                ResultSet results = statement.executeQuery()
        ) {
            int operations = updateInto(results);
            logger.trace("Saved data into table {} in {} operations using connection {}", table, operations, connection);
            return operations;
        } catch (SQLFeatureNotSupportedException fnse) {
            logger.debug("Could not save data into table {} because the driver does not support this method", table, fnse);
            return -1;
        }
    }
}
