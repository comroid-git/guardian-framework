package org.comroid.webkit.socket;

import org.comroid.api.NFunction;
import org.comroid.restless.REST;
import org.comroid.webkit.server.WebSocketConnection;
import org.java_websocket.WebSocket;

import java.util.concurrent.Executor;
import java.util.function.BiFunction;

public final class ConnectionFactory<T extends WebSocketConnection> implements BiFunction<WebSocket, REST.Header.List, T> {
    private static final NFunction.In3<WebSocket, REST.Header.List, Executor, WebSocketConnection> DEFAULT_FACTORY = new Default();
    private final NFunction.In3<WebSocket, REST.Header.List, Executor, T> function;
    private final Executor executor;

    public ConnectionFactory(NFunction.In3<WebSocket, REST.Header.List, Executor, T> function, Executor executor) {
        this.function = function;
        this.executor = executor;
    }

    public static ConnectionFactory<WebSocketConnection> standard(final Executor executor) {
        return new ConnectionFactory<>(DEFAULT_FACTORY, executor);
    }

    @Override
    public T apply(WebSocket webSocket, REST.Header.List headers) {
        return function.apply(webSocket, headers, executor);
    }

    public static class Default implements NFunction.In3<WebSocket, REST.Header.List, Executor, WebSocketConnection> {
        @Override
        public WebSocketConnection apply(WebSocket conn, REST.Header.List headers, Executor executor) {
            try {
                return new WebSocketConnection(conn, headers, executor);
            } catch (Throwable e) {
                throw new RuntimeException("Could not initiate Connection", e);
            }
        }
    }
}
