package org.comroid.restless;

import org.comroid.api.ContextualProvider;
import org.comroid.restless.socket.Websocket;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@SuppressWarnings("rawtypes")
public interface HttpAdapter extends ContextualProvider.This {
    static HttpAdapter autodetect() {
        throw new UnsupportedOperationException();
    }

    default CompletableFuture<? extends Websocket> createWebSocket(Executor executor, URI uri, REST.Header.List headers) {
        return createWebSocket(executor, uri, headers, null);
    }

    CompletableFuture<? extends Websocket> createWebSocket(Executor executor, URI uri, REST.Header.List headers, String preferredSubprotocol);

    CompletableFuture<REST.Response> call(REST.Request request);
}
