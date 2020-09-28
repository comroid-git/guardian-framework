package org.comroid.restless.adapter.okhttp.v4;

import okhttp3.*;
import org.comroid.restless.HttpAdapter;
import org.comroid.restless.REST;
import org.comroid.uniform.SerializationAdapter;

import java.io.IOException;
import java.net.URI;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class OkHttp3Adapter implements HttpAdapter {
    private final OkHttpClient httpClient = new OkHttpClient.Builder().build();

    @Override
    public CompletableFuture<REST.Response> call(REST.Request request, String mimeType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final REST.Method requestMethod = request.getMethod();
                final String requestBody = request.getBody();

                final Request.Builder builder = new Request.Builder().url(request.getEndpoint().getURL())
                        // only support null body for GET method, else throw
                        .method(requestMethod.toString(), (
                                requestBody == null && requestMethod == REST.Method.GET ? null : RequestBody.create(
                                        MediaType.parse(mimeType),
                                        Objects.requireNonNull(requestBody, "Null body not supported with " + requestMethod)
                                )
                        ));

                request.getHeaders().forEach(header -> builder.addHeader(header.getName(), header.getValue()));

                final Request kRequest = builder.build();
                final Call call = httpClient.newCall(kRequest);
                final Response response = call.execute();
                final ResponseBody responseBody = response.body();

                return new REST.Response(response.code(), request.getREST().getSerializationAdapter()
                        .createUniNode(responseBody == null ? null : responseBody.string()));
            } catch (IOException e) {
                throw new RuntimeException("Request failed", e);
            }
        });
    }
}
