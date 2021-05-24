package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.NFunction;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.RefMap;
import org.comroid.mutatio.ref.ReferenceMap;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.webkit.socket.ConnectionFactory;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public final class WebSocketServer extends org.java_websocket.server.WebSocketServer implements Closeable, ContextualProvider.Underlying {
    private static final Logger logger = LogManager.getLogger();
    private final ContextualProvider context;
    private final ConnectionFactory<? extends WebSocketConnection> connectionFactory;
    private final RefMap<WebSocket, WebSocketConnection> activeConnections;
    private final Set<Consumer<WebSocketConnection>> connectionListeners;

    {
        this.activeConnections = new ReferenceMap<>();
        this.connectionListeners = new HashSet<>();
    }

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public RefContainer<?, WebSocketConnection> getActiveConnections() {
        return activeConnections.immutable();
    }

    @Deprecated
    public WebSocketServer(
            ContextualProvider context,
            Executor executor,
            InetAddress inetAddress,
            int port
    ) {
        this(context, executor, inetAddress, port, ConnectionFactory.standard(context));
    }

    @Deprecated
    public <C extends WebSocketConnection> WebSocketServer(
            ContextualProvider context,
            Executor executor,
            InetAddress inetAddress,
            int port,
            NFunction.In3<WebSocket, REST.Header.List, ContextualProvider, ? extends WebSocketConnection> connectionConstructor
    ) {
        this(context, executor, inetAddress, port, new ConnectionFactory<>(connectionConstructor, context));
    }

    @Deprecated
    public <C extends WebSocketConnection> WebSocketServer(
            ContextualProvider context,
            Executor executor,
            InetAddress inetAddress,
            int port,
            ConnectionFactory<C> connectionFactory
    ) {
        super(new InetSocketAddress(inetAddress, port));
        logger.warn("Deprecated Constructor used");

        this.context = context;
        this.connectionFactory = connectionFactory;

        super.start();
        logger.debug("Websocket Server available at ws://{}:{} / ws://{}:{}",
                inetAddress.getHostAddress(), port, inetAddress.getHostName(), port);
    }

    public WebSocketServer(
            ContextualProvider context,
            InetAddress address,
            int port
    ) {
        this(context, new InetSocketAddress(address, port));
    }

    public WebSocketServer(
            ContextualProvider context
    ) {
        this(context, context.requireFromContext(InetSocketAddress.class));
    }

    public WebSocketServer(
            ContextualProvider context,
            InetSocketAddress address
    ) {
        this(context, address, context.requireFromContext(ConnectionFactory.class));
    }

    public WebSocketServer(
            ContextualProvider context,
            InetSocketAddress address,
            ConnectionFactory<? extends WebSocketConnection> connectionFactory
    ) {
        super(address);

        this.context = context;
        this.connectionFactory = connectionFactory;

        super.start();
        logger.debug("Websocket Server available at ws://{}:{} / ws://{}:{}",
                address.getAddress().getHostAddress(), address.getPort(), address.getHostName(), address.getPort());
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
            headers.add(name, handshake.getFieldValue(name).split("[,;\\s]"));
        }

        try {
            WebSocketConnection connection = connectionFactory.apply(conn, headers);
            activeConnections.put(conn, connection);
            publishConnection(connection);
        } catch (Throwable t) {
            logger.fatal("Could not properly accept incoming connection; closing incoming connection", t);
            conn.close(CloseFrame.ABNORMAL_CLOSE, t.getClass().getName() + ": " + t.getMessage());
        }
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
        logger.error("Error ocurred in WebSocketServer", ex);
        WebSocketConnection connection = findConnection(conn);

        connection.publishPacket(WebsocketPacket.error(ex));
    }

    @Override
    @Internal
    public void onStart() {
        logger.debug("Underlying WebSocketServer started");
    }

    private @NotNull WebSocketConnection findConnection(WebSocket conn) throws NoSuchElementException {
        if (conn == null)
            throw new IllegalArgumentException("No connection base provided");
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
