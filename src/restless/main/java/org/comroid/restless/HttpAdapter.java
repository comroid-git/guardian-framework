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

    CompletableFuture<? extends Websocket> createWebSocket(Executor executor, URI uri, REST.Header.List headers);

    CompletableFuture<REST.Response> call(REST.Request request, String mimeType);
}
