package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.RefPipe;
import org.comroid.mutatio.ref.ReferencePipe;
import org.comroid.webkit.socket.ConnectionFactory;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

public class WebsocketServer implements Closeable {
    private static final Logger logger = LogManager.getLogger();
    private final ContextualProvider context;
    private final ScheduledExecutorService executor;
    private final String baseUrl;
    private final InetAddress inetAddress;
    private final int port;
    private final Function<Socket, ? extends WebSocketConnection> connectionFactory;
    private final RefPipe<?, WebSocketConnection, ?, WebSocketConnection> connectionPipeline;
    private final ListenerThread listener;
    private final ServerSocket socket;

    public final RefContainer<?, WebSocketConnection> getConnectionPipeline() {
        return connectionPipeline;
    }

    public WebsocketServer(
            ContextualProvider context,
            ScheduledExecutorService executor,
            String baseUrl,
            InetAddress inetAddress,
            int port
    ) throws IOException {
        this(context, executor, baseUrl, inetAddress, port, ConnectionFactory.standard(executor));
    }

    public WebsocketServer(
            ContextualProvider context,
            ScheduledExecutorService executor,
            String baseUrl,
            InetAddress inetAddress,
            int port,
            Function<Socket, ? extends WebSocketConnection> connectionFactory
    ) throws IOException {
        this.context = context;
        this.executor = executor;
        this.baseUrl = baseUrl;
        this.inetAddress = inetAddress;
        this.port = port;
        this.connectionFactory = connectionFactory;
        this.connectionPipeline = new ReferencePipe<>();
        this.listener = new ListenerThread();
        this.socket = new ServerSocket(port, 50, inetAddress);
        executor.execute(listener);
        System.out.println("socket.isBound() = " + socket.isBound());
        System.out.println("socket.getLocalSocketAddress() = " + socket.getLocalSocketAddress());
        System.out.println("socket.getInetAddress() = " + socket.getInetAddress());
    }

    @Override
    public final void close() throws IOException {
        logger.debug("Closing Server Socket");
        socket.close();
    }

    private final class ListenerThread implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    int c = 0;
                    while (socket.isBound()) {
                        c = 0;
                        try {
                            logger.debug("Waiting for WebSocket client");
                            Socket client = socket.accept();
                            logger.debug("Incoming Client: " + client);
                            WebSocketConnection connection = connectionFactory.apply(client);
                            connectionPipeline.accept(null, connection);
                            logger.debug("Client was handled");
                        } catch (IOException e) {
                            logger.error("Could not handle an incoming client", e);
                        } finally {
                            logger.trace("Client debugging ended");
                        }
                    }
                    logger.warn("Error: Socket is not bound");
                    if (++c < 5) {
                        logger.error("Socket closed; could not reconnect after {} attempts", c);
                        return;
                    }
                }
            } catch (Throwable t) {
                logger.fatal("Fatal error occurred in Socket Listener", t);
            }
        }
    }
}
