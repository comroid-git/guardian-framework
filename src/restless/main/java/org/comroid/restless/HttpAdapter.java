package org.comroid.restless;

import org.comroid.annotations.Upgrade;
import org.comroid.api.ContextualProvider;
import org.comroid.restless.socket.Websocket;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@SuppressWarnings("rawtypes")
public interface HttpAdapter extends ContextualProvider.This<Object> {
    static HttpAdapter autodetect() {
        throw new UnsupportedOperationException();
    }

    @Upgrade
    static HttpAdapter upgrade(ContextualProvider context) {
        return context.getFromContext(HttpAdapter.class).orElseThrow(() ->
                new NoSuchElementException("Could not find any HttpAdapter in Context"));
    }

    default CompletableFuture<? extends Websocket> createWebSocket(
            Executor executor,
            Consumer<Throwable> exceptionHandler,
            URI uri,
            REST.Header.List headers
    ) {
        return createWebSocket(executor, exceptionHandler, uri, headers, null);
    }

    CompletableFuture<? extends Websocket> createWebSocket(
            Executor executor,
            Consumer<Throwable> exceptionHandler,
            URI uri,
            REST.Header.List headers,
            String preferredSubprotocol
    );

    CompletableFuture<REST.Response> call(REST.Request request);
}
