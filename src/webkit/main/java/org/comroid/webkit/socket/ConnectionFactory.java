package org.comroid.webkit.socket;

import org.comroid.api.ContextualProvider;
import org.comroid.api.NFunction;
import org.comroid.restless.REST;
import org.comroid.webkit.server.WebSocketConnection;
import org.java_websocket.WebSocket;

import java.util.function.BiFunction;

public final class ConnectionFactory<T extends WebSocketConnection> implements BiFunction<WebSocket, REST.Header.List, T> {
    private static final NFunction.In3<WebSocket, REST.Header.List, ContextualProvider, WebSocketConnection> DEFAULT_FACTORY = new Default();
    private final NFunction.In3<WebSocket, REST.Header.List, ContextualProvider, T> function;
    private final ContextualProvider context;

    public ConnectionFactory(NFunction.In3<WebSocket, REST.Header.List, ContextualProvider, T> function, ContextualProvider context) {
        this.function = function;
        this.context = context;
    }

    public static ConnectionFactory<WebSocketConnection> standard(final ContextualProvider executor) {
        return new ConnectionFactory<>(DEFAULT_FACTORY, executor);
    }

    @Override
    public T apply(WebSocket webSocket, REST.Header.List headers) {
        return function.apply(webSocket, headers, context);
    }

    public static class Default implements NFunction.In3<WebSocket, REST.Header.List, ContextualProvider, WebSocketConnection> {
        @Override
        public WebSocketConnection apply(WebSocket conn, REST.Header.List headers, ContextualProvider context) {
            try {
                return new WebSocketConnection(conn, headers, context) {
                };
            } catch (Throwable e) {
                throw new RuntimeException("Could not initiate Connection", e);
            }
        }
    }
}
