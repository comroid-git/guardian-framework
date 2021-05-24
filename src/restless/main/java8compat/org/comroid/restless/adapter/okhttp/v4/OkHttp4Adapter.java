package org.comroid.restless.adapter.okhttp.v4;

import okhttp3.*;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.HttpAdapter;
import org.comroid.restless.REST;
import org.comroid.uniform.SerializationAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class OkHttp4Adapter implements HttpAdapter {
    private final OkHttpClient httpClient = new OkHttpClient.Builder().build();

    @Override
    public CompletableFuture<OkHttp4WebSocket> createWebSocket(Executor executor, Consumer<Throwable> exceptionHandler, URI uri, REST.Header.List headers, String preferredProtocol) {
        return CompletableFuture.completedFuture(new OkHttp4WebSocket(httpClient, executor, exceptionHandler, uri, headers, preferredProtocol));
    }

    @Override
    public CompletableFuture<REST.Response> call(REST.Request request) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final REST.Method requestMethod = request.getMethod();
                String requestBody = null;
                if (request.getBody() != null) {
                    requestBody = request.getBody().toSerializedString();
                }

                final Request.Builder builder = new Request.Builder().url(request.getEndpoint().getURL());

                final MediaType mediaType = MediaType.parse(request.getHeaders().getFirst(CommonHeaderNames.REQUEST_CONTENT_TYPE));
                builder.method(requestMethod.name(), requestBody == null ? null : RequestBody.create(mediaType, requestBody));

                request.getHeaders().forEach(header -> builder.addHeader(header.getName(), header.combineValues()));

                final Request kRequest = builder.build();
                final Call call = httpClient.newCall(kRequest);
                final Response response = call.execute();
                final ResponseBody responseBody = response.body();

                return new REST.Response(response.code(), request.getREST()
                        .requireFromContext(SerializationAdapter.class)
                        .createUniNode(responseBody == null ? null : responseBody.string()));
            } catch (IOException e) {
                throw new RuntimeException("Request failed", e);
            }
        });
    }
}
