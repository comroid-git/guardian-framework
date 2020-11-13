package org.comroid.restless.adapter.jdk;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.pump.Pump;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebSocket;
import org.comroid.restless.socket.WebSocketPacket;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

public final class JavaWebSocket implements WebSocket {
    private final Executor executor;
    private final URI uri;
    private final Pump<? extends WebSocketPacket> pump;

    @Override
    public Pump<? extends WebSocketPacket> getPacketPump() {
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

    JavaWebSocket(HttpClient httpClient, Executor executor, URI uri, REST.Header.List headers) {
        this.executor = executor;
        this.pump = Pump.create(executor);
        this.uri = uri;

        java.net.http.WebSocket.Builder socketBuilder = httpClient.newWebSocketBuilder();
        headers.forEach(socketBuilder::header);

        socketBuilder.buildAsync(uri, new Listener())
                .exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });
    }

    private void feed(WebSocketPacket packet) {
        pump.accept(Reference.constant(packet));
    }

    private class Listener implements java.net.http.WebSocket.Listener {
        private Reference<StringBuilder> builder = Reference.create(new StringBuilder());

        @Override
        public void onOpen(java.net.http.WebSocket webSocket) {
            feed(new WebSocketPacket.Empty(WebSocketPacket.Type.OPEN));
        }

        @Override
        public CompletionStage<?> onText(java.net.http.WebSocket webSocket, CharSequence data, boolean last) {
            builder.consume(sb -> sb.append(data));
            if (last) pushData();

            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onBinary(java.net.http.WebSocket webSocket, ByteBuffer data, boolean last) {
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
        public CompletionStage<?> onPing(java.net.http.WebSocket webSocket, ByteBuffer message) {
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
        public CompletionStage<?> onPong(java.net.http.WebSocket webSocket, ByteBuffer message) {
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
        public void onError(java.net.http.WebSocket webSocket, Throwable error) {
            feed(new WebSocketPacket.Empty(WebSocketPacket.Type.ERROR) {
                @Override
                public Rewrapper<Throwable> getError() {
                    return () -> error;
                }
            });

            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onClose(java.net.http.WebSocket webSocket, int statusCode, String reason) {
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
