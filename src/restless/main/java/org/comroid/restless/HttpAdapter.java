package org.comroid.restless;

import org.comroid.api.ContextualTypeProvider;
import org.comroid.restless.socket.WebSocket;
import org.comroid.uniform.SerializationAdapter;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@SuppressWarnings("rawtypes")
public interface HttpAdapter extends ContextualTypeProvider.This<HttpAdapter> {
    static HttpAdapter autodetect() {
        throw new UnsupportedOperationException();
    }

    CompletableFuture<? extends WebSocket> createWebSocket(Executor executor, URI uri, REST.Header.List headers);

    CompletableFuture<REST.Response> call(REST.Request request, String mimeType);
}
