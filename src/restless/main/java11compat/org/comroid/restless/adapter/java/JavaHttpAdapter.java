package org.comroid.restless.adapter.java;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.restless.HttpAdapter;
import org.comroid.restless.REST;
import org.comroid.restless.socket.Websocket;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class JavaHttpAdapter implements HttpAdapter {
    private static final Logger logger = LogManager.getLogger();
    private final HttpClient httpClient;

    public JavaHttpAdapter() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    @Override
    public CompletableFuture<? extends Websocket> createWebSocket(
            Executor executor,
            Consumer<Throwable> exceptionHandler,
            URI uri,
            REST.Header.List headers,
            String preferredProtocol
    ) {
        return CompletableFuture.completedFuture(new JavaWebsocket(httpClient, executor, exceptionHandler, uri, headers, preferredProtocol));
    }

    @Override
    public CompletableFuture<REST.Response> call(REST.Request request) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder(request.getEndpoint().getURI());

        request.getHeaders().forEach(builder::header);
        final HttpRequest.BodyPublisher publisher = request.getMethod() == REST.Method.GET
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(request.getBody(), StandardCharsets.UTF_8);
        builder.method(request.getMethod().name(), publisher);

        return httpClient.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(response -> {
                    final String body = response.body();

                    if (body == null)
                        return new REST.Response(response.statusCode());

                    final UniNode data = request.getREST()
                            .requireFromContext(SerializationAdapter.class)
                            .createUniNode(body);

                    return new REST.Response(response.statusCode(), data);
                });
    }
}
