package org.comroid.webkit.socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.restless.MimeType;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.uniform.node.UniNode;
import org.comroid.webkit.server.WebSocketConnection;
import org.java_websocket.WebSocket;

public abstract class WebkitConnection extends WebSocketConnection {
    private static final Logger logger = LogManager.getLogger();
    public final String host;

    public WebkitConnection(
            WebSocket socketBase,
            REST.Header.List headers,
            ContextualProvider context
    ) {
        super(socketBase, headers, context);
        this.host = getHeaders().getFirst("Host");

        on(WebsocketPacket.Type.DATA)
                .flatMap(WebsocketPacket::getData)
                .yield(str -> !str.startsWith("hello"), str -> sendText("hello client"))
                .map(findSerializer()::parse)
                .peek(command -> {
                    try {
                        handleCommand(command);
                    } catch (Exception e) {
                        logger.error("Error ocurred in command handler", e);
                    }
                });
    }

    protected final Serializer<UniNode> findSerializer() {
        return findSerializer(MimeType.JSON);
    }

    protected abstract void handleCommand(UniNode command);
}
