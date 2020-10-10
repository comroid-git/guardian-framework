package org.comroid.restless;

import org.comroid.api.ContextualTypeProvider;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("rawtypes")
public interface HttpAdapter extends ContextualTypeProvider.This<HttpAdapter> {
    static HttpAdapter autodetect() {
        throw new UnsupportedOperationException();
    }

    //CompletableFuture<? extends WebSocket> createWebSocket(SerializationAdapter<?, ?, ?> seriLib, Executor executor, URI uri, REST.Header.List headers);

    CompletableFuture<REST.Response> call(REST.Request request, String mimeType);
}
