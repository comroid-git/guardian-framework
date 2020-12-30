package org.comroid.restless.adapter.jdk;

import com.google.common.flogger.FluentLogger;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.pipe.Pipe;
import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.ref.FutureReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.restless.socket.Websocket;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public final class JavaWebsocket implements Websocket {
    public static final FluentLogger LOGGER = FluentLogger.forEnclosingClass();
    private final Executor executor;
    private final URI uri;
    private final Pump<? extends WebsocketPacket> pump;
    private final Pipe<? extends WebsocketPacket> pipeline;
    private final FutureReference<WebSocket> jSocket = new FutureReference<>();

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

    @Override
    public CompletableFuture<Websocket> send(String[] splitMessage) {
        final WebSocket jSocket = this.jSocket.requireNonNull("Socket not available");

        for (int i = 0; i < splitMessage.length; i++)
            jSocket.sendText(splitMessage[i], i == splitMessage.length - 1);

        return CompletableFuture.completedFuture(this);
    }

    JavaWebsocket(HttpClient httpClient, Executor executor, URI uri, REST.Header.List headers) {
        this.executor = executor;
        this.uri = uri;
        this.pump = Pump.create(executor);
        this.pipeline = pump.peek(packet -> LOGGER.atFinest().log("WebSocket received packet: %s", packet));

        WebSocket.Builder socketBuilder = httpClient.newWebSocketBuilder();
        headers.forEach(socketBuilder::header);

        socketBuilder.buildAsync(uri, new Listener())
                .thenAccept(jSocket.future::complete)
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
    }

    private void feed(WebsocketPacket packet) {
        pump.accept(Reference.constant(packet));
    }

    @Override
    public void close() throws IOException {
        jSocket.future.join().sendClose(1000, "Websocket Closed");
    }

    private class Listener implements WebSocket.Listener {
        private Reference<StringBuilder> builder = Reference.create(new StringBuilder());

        @Override
        public void onOpen(WebSocket webSocket) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.OPEN));
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            builder.consume(sb -> sb.append(data));
            if (last) pushData();

            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
            builder.consume(sb -> sb.append(data.toString()));
            if (last) pushData();

            webSocket.request(1);
            return null;
        }

        public void pushData() {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.DATA) {
                final Processor<String> string = builder.map(StringBuilder::toString);

                @Override
                public Rewrapper<String> getData() {
                    return string;
                }
            });
            builder = Reference.create(new StringBuilder());
        }

        @Override
        public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.PING) {
                final String data = message.toString();

                @Override
                public Rewrapper<String> getData() {
                    return () -> data;
                }
            });

            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.PONG) {
                final String data = message.toString();

                @Override
                public Rewrapper<String> getData() {
                    return () -> data;
                }
            });

            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.ERROR) {
                @Override
                public Rewrapper<Throwable> getError() {
                    return () -> error;
                }
            });

            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.CLOSE) {
                @Override
                public Rewrapper<String> getData() {
                    return () -> reason;
                }

                @Override
                public Rewrapper<Integer> getStatusCode() {
                    return () -> statusCode;
                }
            });

            return null;
        }
    }
}
