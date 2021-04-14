package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.webkit.socket.ConnectionFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiFunction;

public class WebSocketServer extends org.java_websocket.server.WebSocketServer implements Closeable {
    private static final Logger logger = LogManager.getLogger();
    private final ContextualProvider context;
    private final ScheduledExecutorService executor;
    private final String baseUrl;
    private final BiFunction<WebSocket, REST.Header.List, ? extends WebSocketConnection> connectionFactory;
    private final Map<WebSocket, WebSocketConnection> activeConnections;

    public WebSocketServer(
            ContextualProvider context,
            ScheduledExecutorService executor,
            String baseUrl,
            InetAddress inetAddress,
            int port
    ) throws IOException {
        this(context, executor, baseUrl, inetAddress, port, ConnectionFactory.standard(executor));
    }

    public <C extends WebSocketConnection> WebSocketServer(
            ContextualProvider context,
            ScheduledExecutorService executor,
            String baseUrl,
            InetAddress inetAddress,
            int port,
            BiFunction<WebSocket, REST.Header.List, C> connectionFactory
    ) throws IOException {
        super(new InetSocketAddress(inetAddress, port));

        this.context = context;
        this.executor = executor;
        this.baseUrl = baseUrl;
        this.connectionFactory = connectionFactory;
        this.activeConnections = new ConcurrentHashMap<>();

        super.start();
    }

    @Override
    public final void close() throws IOException {
        logger.debug("Closing WebSocketServer");
        try {
            logger.trace("Closing {} active connections", activeConnections.size());
            for (WebSocketConnection connection : activeConnections.values())
                connection.close();
            logger.trace("Stopping underlying Server");
            super.stop();
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        Iterator<String> headerIter = handshake.iterateHttpFields();
        REST.Header.List headers = new REST.Header.List();

        while (headerIter.hasNext()) {
            String name = headerIter.next();
            assert headerIter.hasNext() : "Headers incomplete";
            String values = headerIter.next();

            headers.add(name, values.split(","));
        }

        WebSocketConnection connection = connectionFactory.apply(conn, headers);
        activeConnections.put(conn, connection);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        WebSocketConnection connection = findConnection(conn);

        if (remote)
            connection.nowClosed(code, reason);
        activeConnections.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        WebSocketConnection connection = findConnection(conn);

        connection.publishPacket(WebsocketPacket.data(message));
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        WebSocketConnection connection = findConnection(conn);

        connection.publishPacket(WebsocketPacket.error(ex));
    }

    @Override
    public void onStart() {
        logger.debug("Underlying WebSocketServer started");
    }

    private @NotNull WebSocketConnection findConnection(WebSocket conn) throws NoSuchElementException {
        WebSocketConnection found = activeConnections.getOrDefault(conn, null);
        if (found == null)
            throw new NoSuchElementException("No active connection available for " + conn);
        return found;
    }
}
