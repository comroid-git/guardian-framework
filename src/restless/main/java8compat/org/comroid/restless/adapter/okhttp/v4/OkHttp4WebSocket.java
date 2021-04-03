package org.comroid.restless.adapter.okhttp.v4;

import okhttp3.*;
import okio.ByteString;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.model.RefPipe;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferencePipe;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.REST;
import org.comroid.restless.socket.Websocket;
import org.comroid.restless.socket.WebsocketPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class OkHttp4WebSocket implements Websocket {
    private static final Logger logger = LogManager.getLogger();
    private final Executor executor;
    private final URI uri;
    private final RefPipe<WebsocketPacket.Type, WebsocketPacket, WebsocketPacket.Type, ? extends WebsocketPacket> pipeline;
    private final WebSocket internalSocket;

    @Override
    public RefPipe<?, ?, WebsocketPacket.Type, ? extends WebsocketPacket> getPacketPipeline() {
        return pipeline;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    OkHttp4WebSocket(OkHttpClient httpClient, Executor executor, Consumer<Throwable> exceptionHandler, URI uri, REST.Header.List headers, String preferredProtocol) {
        final Request.Builder initBuilder = new Request.Builder().url(uri.toString());
        if (preferredProtocol != null)
            headers.add(CommonHeaderNames.WEBSOCKET_SUBPROTOCOL, preferredProtocol);
        headers.forEach((name, value) -> {
            try {
                initBuilder.addHeader(name, value);
            } catch (Throwable t) {
                throw new RuntimeException(String.format("Problem with header: %s = %s", name, value), t);
            }
        });

        this.executor = executor;
        this.uri = uri;
        this.pipeline = new ReferencePipe<>(executor);
        this.internalSocket = httpClient.newWebSocket(initBuilder.build(), new Listener());
    }

    @Override
    public CompletableFuture<Websocket> send(String[] splitMessage) {
        return send(String.join("", splitMessage));
    }

    @Override
    public CompletableFuture<Websocket> send(String data, int maxLength) {
        return send(data);
    }

    @Override
    public CompletableFuture<Websocket> send(String data) {
        logger.trace("{} - Sending Socket message {}", getName(), data);
        if (!internalSocket.send(data)) {
            RuntimeException exception = new RuntimeException(String.format("%s - Shutting down due to an error", getName()));
            logger.fatal("Could not send data! Websocket will shut down", exception);
            return Polyfill.failedFuture(exception);
        }
        return CompletableFuture.completedFuture(this);
    }

    private void feed(WebsocketPacket packet) {
        pipeline.accept(packet.getType(), packet);
    }

    @Override
    public void close() throws IOException {
        internalSocket.close(1000, String.format("%s - Shutting down", getName()));
        pipeline.close();
    }

    private class Listener extends WebSocketListener {
        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.OPEN));
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, final @NotNull String text) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.DATA) {
                @Override
                public Rewrapper<String> getData() {
                    return () -> text;
                }
            });
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, final @NotNull ByteString bytes) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.DATA) {
                @Override
                public Rewrapper<String> getData() {
                    return () -> new String(bytes.toByteArray());
                }
            });
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, final @NotNull Throwable t, @Nullable Response response) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.ERROR) {
                @Override
                public Rewrapper<Throwable> getError() {
                    return () -> t;
                }
            });
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, final int code, final @NotNull String reason) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.CLOSE) {
                @Override
                public Rewrapper<Integer> getStatusCode() {
                    return () -> code;
                }

                @Override
                public Rewrapper<String> getData() {
                    return () -> reason;
                }
            });
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, final int code, final @NotNull String reason) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.CLOSE) {
                @Override
                public Rewrapper<Integer> getStatusCode() {
                    return () -> code;
                }

                @Override
                public Rewrapper<String> getData() {
                    return () -> reason;
                }
            });
        }
    }
}
