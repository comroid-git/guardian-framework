package org.comroid.restless.adapter.java;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.RefPipe;
import org.comroid.mutatio.ref.FutureReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferencePipe;
import org.comroid.restless.REST;
import org.comroid.restless.socket.Websocket;
import org.comroid.restless.socket.WebsocketPacket;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class JavaWebsocket implements Websocket {
    private static final Logger logger = LogManager.getLogger();
    private final HttpClient httpClient;
    private final Executor executor;
    private final URI uri;
    private final RefPipe<WebsocketPacket.Type, WebsocketPacket, WebsocketPacket.Type, WebsocketPacket> pipeline;
    private final REST.Header.List headers;
    private final String preferredProtocol;
    private final FutureReference<WebSocket> jSocket = new FutureReference<>();

    @Override
    public RefContainer<WebsocketPacket.Type, WebsocketPacket> getEventPipeline() {
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

    JavaWebsocket(HttpClient httpClient, Executor executor, Consumer<Throwable> exceptionHandler, URI uri, REST.Header.List headers, String preferredProtocol) {
        this.httpClient = httpClient;
        this.executor = executor;
        this.uri = uri;
        this.pipeline = new ReferencePipe<>(executor);
        this.headers = headers;
        this.preferredProtocol = preferredProtocol;
    }

    @Override
    public CompletableFuture<Websocket> send(String[] splitMessage) {
        final WebSocket jSocket = this.jSocket.requireNonNull("Socket not available");

        logger.trace("{} - Sending Socket message: {}", getName(), Arrays.toString(splitMessage));
        for (int i = 0; i < splitMessage.length; i++)
            jSocket.sendText(splitMessage[i], i == splitMessage.length - 1);

        return CompletableFuture.completedFuture(this);
    }

    @Override
    public CompletableFuture<? extends Websocket> open() {
        if (jSocket.future.isDone())
            throw new IllegalStateException("Websocket is already open");

        WebSocket.Builder socketBuilder = httpClient.newWebSocketBuilder();
        headers.forEach(socketBuilder::header);
        if (preferredProtocol != null)
            socketBuilder.subprotocols(preferredProtocol);
        return socketBuilder.buildAsync(uri, new Listener())
                .thenAccept(jSocket.future::complete)
                .thenApply(nil -> this)
                .exceptionally(Polyfill.exceptionLogger(logger, "Error while building WebSocket"));
    }

    private void feed(WebsocketPacket packet) {
        try {
            pipeline.accept(packet.getType(), packet);
        } catch (Throwable t) {
            logger.error(String.format("%s - A problem occurred when feeding packet %s", getName(), packet), t);
        }
    }

    @Override
    public void close() throws IOException {
        jSocket.future.join().sendClose(1000, String.format("%s - Socket Closed", getName()));
        pipeline.close();
    }

    private class Listener implements WebSocket.Listener {
        private Reference<StringBuilder> builder = Reference.create(new StringBuilder());

        @Override
        public void onOpen(WebSocket webSocket) {
            feed(new WebsocketPacket.Empty(WebsocketPacket.Type.OPEN));
            webSocket.request(1);
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
                final Reference<String> string = builder.map(StringBuilder::toString);

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
