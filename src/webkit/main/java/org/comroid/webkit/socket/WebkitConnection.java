package org.comroid.webkit.socket;

import org.comroid.restless.REST;
import org.comroid.webkit.server.WebSocketConnection;
import org.java_websocket.WebSocket;

import java.util.concurrent.Executor;

public class WebkitConnection extends WebSocketConnection {
    public WebkitConnection(
            WebSocket socketBase,
            REST.Header.List headers,
            Executor executor
    ) {
        super(socketBase, headers, executor);
    }
}
