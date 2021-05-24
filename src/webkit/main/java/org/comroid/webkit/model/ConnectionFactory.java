package org.comroid.webkit.model;

import org.comroid.api.ContextualProvider;
import org.comroid.restless.REST;
import org.comroid.webkit.server.WebSocketConnection;
import org.comroid.webkit.socket.ConnectionFactoryBase;
import org.java_websocket.WebSocket;

import java.util.function.BiFunction;

public interface ConnectionFactory<T extends WebSocketConnection> extends BiFunction<WebSocket, REST.Header.List, T> {
    static ConnectionFactory<WebSocketConnection> standard(final ContextualProvider context) {
        return new ConnectionFactoryBase<>(ConnectionFactoryBase.DEFAULT_FACTORY, context);
    }
}
