package org.comroid.restless.adapter.jdk;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.ref.FutureReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebSocketPacket;
import org.comroid.restless.socket.Websocket;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public final class JavaWebsocket implements Websocket {
    private final Executor executor;
    private final URI uri;
    private final Pump<? extends WebSocketPacket> pump;
    private final FutureReference<WebSocket> jSocket = new FutureReference<>();

    @Override
    public Pump<? extends WebSocketPacket> getPacketPipeline() {
        return pump;
    }

    @Override
    public URI getURI() {
        return uri;
    }

    @Override
    public Executor getExecutor() {
        return executor;
    }

    JavaWebsocket(HttpClient httpClient, Executor executor, URI uri, REST.Header.List headers) {
        this.executor = executor;
        this.uri = uri;
        this.pump = Pump.create(executor);

        WebSocket.Builder socketBuilder = httpClient.newWebSocketBuilder();
        headers.forEach(socketBuilder::header);

        socketBuilder.buildAsync(uri, new Listener())
                .thenAccept(jSocket.future::complete)
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
    }

    private void feed(WebSocketPacket packet) {
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
            feed(new WebSocketPacket.Empty(WebSocketPacket.Type.OPEN));
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
            feed(new WebSocketPacket.Empty(WebSocketPacket.Type.DATA) {
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
            feed(new WebSocketPacket.Empty(WebSocketPacket.Type.PING) {
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
            feed(new WebSocketPacket.Empty(WebSocketPacket.Type.PONG) {
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
            feed(new WebSocketPacket.Empty(WebSocketPacket.Type.ERROR) {
                @Override
                public Rewrapper<Throwable> getError() {
                    return () -> error;
                }
            });

            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            feed(new WebSocketPacket.Empty(WebSocketPacket.Type.CLOSE) {
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
