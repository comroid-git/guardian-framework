package org.comroid.webkit.socket;

import org.comroid.webkit.server.WebSocketConnection;

import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ConnectionFactory<T extends WebSocketConnection> implements Function<Socket, T> {
    private static final BiFunction<Socket, Executor, WebSocketConnection> DEFAULT_FACTORY = (socket, executor) -> {
        try {
            return new WebSocketConnection(socket, executor);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("Connection is not set up correctly", e);
        } catch (IOException e) {
            throw new RuntimeException("Could not initiate Connection", e);
        }
    };
    private final BiFunction<Socket, Executor, T> function;
    private final Executor executor;

    public ConnectionFactory(BiFunction<Socket, Executor, T> function, Executor executor) {
        this.function = function;
        this.executor = executor;
    }

    public static ConnectionFactory<WebSocketConnection> standard(final Executor executor) {
        return new ConnectionFactory<>(DEFAULT_FACTORY, executor);
    }

    @Override
    public T apply(Socket socket) {
        return function.apply(socket, executor);
    }
}
