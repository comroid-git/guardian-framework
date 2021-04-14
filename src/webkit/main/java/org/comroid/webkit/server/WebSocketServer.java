package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.NFunction;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.webkit.socket.ConnectionFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class WebSocketServer extends org.java_websocket.server.WebSocketServer implements Closeable, ContextualProvider.Underlying {
    private static final Logger logger = LogManager.getLogger();
    private final ContextualProvider context;
    private final Executor executor;
    private final String baseUrl;
    private final ConnectionFactory<? extends WebSocketConnection> connectionFactory;
    private final Map<WebSocket, WebSocketConnection> activeConnections;
    private final Set<Consumer<WebSocketConnection>> connectionListeners;

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public Executor getExecutor() {
        return executor;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public WebSocketServer(
            ContextualProvider context,
            Executor executor,
            String baseUrl,
            InetAddress inetAddress,
            int port
    ) {
        this(context, executor, baseUrl, inetAddress, port, ConnectionFactory.standard(executor));
    }

    public <C extends WebSocketConnection> WebSocketServer(
            ContextualProvider context,
            Executor executor,
            String baseUrl,
            InetAddress inetAddress,
            int port,
            NFunction.In3<WebSocket, REST.Header.List, Executor, ? extends WebSocketConnection> connectionConstructor
    ) {
        this(context, executor, baseUrl, inetAddress, port, new ConnectionFactory<>(connectionConstructor, executor));
    }

    public <C extends WebSocketConnection> WebSocketServer(
            ContextualProvider context,
            Executor executor,
            String baseUrl,
            InetAddress inetAddress,
            int port,
            ConnectionFactory<C> connectionFactory
    ) {
        super(new InetSocketAddress(inetAddress, port));

        this.context = context;
        this.executor = executor;
        this.baseUrl = baseUrl;
        this.connectionFactory = connectionFactory;
        this.activeConnections = new ConcurrentHashMap<>();
        this.connectionListeners = new HashSet<>();

        super.start();
    }

    public BooleanSupplier onNewConnection(Consumer<WebSocketConnection> listener) {
        if (!connectionListeners.add(listener))
            throw new RuntimeException("Could not add Connection listener");
        return () -> connectionListeners.remove(listener);
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
    @Internal
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
        publishConnection(connection);
    }

    @Override
    @Internal
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        WebSocketConnection connection = findConnection(conn);

        if (remote)
            connection.nowClosed(code, reason);
        activeConnections.remove(conn);
    }

    @Override
    @Internal
    public void onMessage(WebSocket conn, String message) {
        WebSocketConnection connection = findConnection(conn);

        connection.publishPacket(WebsocketPacket.data(message));
    }

    @Override
    @Internal
    public void onError(WebSocket conn, Exception ex) {
        WebSocketConnection connection = findConnection(conn);

        connection.publishPacket(WebsocketPacket.error(ex));
    }

    @Override
    @Internal
    public void onStart() {
        logger.debug("Underlying WebSocketServer started");
    }

    private @NotNull WebSocketConnection findConnection(WebSocket conn) throws NoSuchElementException {
        WebSocketConnection found = activeConnections.getOrDefault(conn, null);
        if (found == null)
            throw new NoSuchElementException("No active connection available for " + conn);
        return found;
    }

    private void publishConnection(WebSocketConnection connection) {
        for (Consumer<WebSocketConnection> listener : connectionListeners)
            listener.accept(connection);
    }
}