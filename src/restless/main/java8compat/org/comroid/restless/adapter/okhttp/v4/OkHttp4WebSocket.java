package org.comroid.restless.adapter.okhttp.v4;

import com.google.common.flogger.FluentLogger;
import okhttp3.*;
import okio.ByteString;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.restless.socket.Websocket;
import org.comroid.restless.socket.WebsocketPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public final class OkHttp4WebSocket implements Websocket {
    private final static FluentLogger LOGGER = FluentLogger.forEnclosingClass();
    private final Executor executor;
    private final URI uri;
    private final Pump<? extends WebsocketPacket> pump;
    private final Pipe<? extends WebsocketPacket> pipeline;
    private final WebSocket internalSocket;

    @Override
    public Pipe<? extends WebsocketPacket> getPacketPipeline() {
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

    OkHttp4WebSocket(OkHttpClient httpClient, Executor executor, URI uri, REST.Header.List headers) {
        final Request.Builder initBuilder = new Request.Builder().url(uri.toString());
        headers.forEach(initBuilder::addHeader);

        this.executor = executor;
        this.uri = uri;
        this.pump = Pump.create(executor);
        this.pipeline = pump.peek(packet -> LOGGER.atInfo().log("WebSocket received packet: %s", packet));
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
        if (!internalSocket.send(data))
            return Polyfill.failedFuture(new RuntimeException("WebSocket shutting down due to an error"));
        return CompletableFuture.completedFuture(this);
    }

    @Override
    public void close() throws IOException {
        internalSocket.close(1000, "WebSocket shutting down");
        pump.close();
    }

    private void feed(WebsocketPacket packet) {
        pump.accept(Reference.constant(packet));
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
