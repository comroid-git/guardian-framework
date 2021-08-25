package org.comroid.varbind;

import org.comroid.api.ContextualProvider;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.ApiStatus.Experimental;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseCache<K, V extends DataContainer<? super V>> extends DataContainerCache<K, V> {
    private final Connection connection;

    public DatabaseCache(ContextualProvider context, Connection connection, int largeThreshold, VarBind<? super V, K, ?, K> idBind) {
        super(context, largeThreshold, idBind);
        this.connection = connection;
    }

    public DatabaseCache(ContextualProvider context, Connection connection, int largeThreshold, VarBind<? super V, K, ?, K> idBind, String idColumn) {
        super(context, largeThreshold, idBind, idColumn);
        this.connection = connection;
    }

    @Experimental
    public final int reloadData(String table) throws SQLException {
        return reloadData(connection, table);
    }

    @Experimental
    public final int saveData(String table) throws SQLException {
        return saveData(connection, table);
    }
}
