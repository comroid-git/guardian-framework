package org.comroid.restless;

import com.google.common.flogger.FluentLogger;
import com.sun.net.httpserver.Headers;
import org.comroid.api.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.body.BodyBuilderType;
import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.restless.endpoint.CompleteEndpoint;
import org.comroid.restless.endpoint.RatelimitedEndpoint;
import org.comroid.restless.server.Ratelimiter;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.comroid.varbind.container.DataContainerBase;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.logging.Level;

import static org.comroid.mutatio.proc.Processor.ofConstant;

public final class REST<D> {
    public static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final HttpAdapter httpAdapter;
    private final SerializationAdapter<?, ?, ?> serializationAdapter;
    private final Ratelimiter ratelimiter;
    private final Executor executor;
    private final D dependency;

    public HttpAdapter getHttpAdapter() {
        return httpAdapter;
    }

    public SerializationAdapter<?, ?, ?> getSerializationAdapter() {
        return serializationAdapter;
    }

    public Ratelimiter getRatelimiter() {
        return ratelimiter;
    }

    public final Executor getExecutor() {
        return executor;
    }

    public REST(
            HttpAdapter httpAdapter,
            SerializationAdapter<?, ?, ?> serializationAdapter
    ) {
        this(httpAdapter, serializationAdapter, (D) null);
    }

    public REST(
            HttpAdapter httpAdapter,
            SerializationAdapter<?, ?, ?> serializationAdapter,
            D dependency
            ) {
        this(httpAdapter, serializationAdapter, dependency, ForkJoinPool.commonPool());
    }

    public REST(
            HttpAdapter httpAdapter,
            SerializationAdapter<?, ?, ?> serializationAdapter,
            Executor requestExecutor
    ) {
        this(httpAdapter, serializationAdapter, null, requestExecutor);
    }

    public REST(
            HttpAdapter httpAdapter,
            SerializationAdapter<?, ?, ?> serializationAdapter,
            D dependency,
            Executor requestExecutor
    ) {
        this(
                httpAdapter,
                serializationAdapter,
                dependency,
                requestExecutor,
                Ratelimiter.INSTANT
        );
    }

    public REST(
            HttpAdapter httpAdapter,
            SerializationAdapter<?, ?, ?> serializationAdapter,
            ScheduledExecutorService scheduledExecutorService,
            RatelimitedEndpoint... pool
    ) {
        this(httpAdapter, serializationAdapter, null, scheduledExecutorService, pool);
    }

    public REST(
            HttpAdapter httpAdapter,
            SerializationAdapter<?, ?, ?> serializationAdapter,
            D dependency,
            ScheduledExecutorService scheduledExecutorService,
            RatelimitedEndpoint... pool
    ) {
        this(
                httpAdapter,
                serializationAdapter,
                dependency,
                scheduledExecutorService,
                Ratelimiter.ofPool(scheduledExecutorService, pool)
        );
    }

    public REST(
            HttpAdapter httpAdapter,
            SerializationAdapter<?, ?, ?> serializationAdapter,
            Executor requestExecutor,
            Ratelimiter ratelimiter
    ) {
        this(httpAdapter, serializationAdapter, null, requestExecutor, ratelimiter);
    }

    public REST(
            HttpAdapter httpAdapter,
            SerializationAdapter<?, ?, ?> serializationAdapter,
            D dependency,
            Executor requestExecutor,
            Ratelimiter ratelimiter
    ) {
        this.httpAdapter = Objects.requireNonNull(httpAdapter, "HttpAdapter");
        this.serializationAdapter = Objects.requireNonNull(serializationAdapter, "SerializationAdapter");
        this.dependency = dependency;
        this.executor = Objects.requireNonNull(requestExecutor, "RequestExecutor");
        this.ratelimiter = Objects.requireNonNull(ratelimiter, "Ratelimiter");
    }

    public Request<UniObjectNode> request() {
        return new Request<>(Invocable.paramReturning(UniObjectNode.class));
    }

    public <T extends DataContainer<? super T>> Request<T> request(Class<T> type) {
        return request(DataContainerBase.findRootBind(type));
    }

    public <T extends DataContainer<? super T>> Request<T> request(GroupBind<T> group) {
        //noinspection unchecked
        return request((Invocable<T>) Polyfill.uncheckedCast(group.getConstructor()
                .orElseThrow(() -> new NoSuchElementException("No constructor applied to GroupBind"))));
    }

    public <T> Request<T> request(Invocable<T> creator) {
        return new Request<>(creator);
    }

    public enum Method {
        GET,

        PUT,

        POST,

        PATCH,

        DELETE,

        HEAD;

        @Override
        public String toString() {
            return name();
        }
    }

    public static final class Header {
        private final String name;
        private final String value;

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public Header(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public static final class List extends ArrayList<Header> {
            public static List of(Headers headers) {
                final List list = new List();

                headers.forEach((name, values) -> list
                        .add(name, values.size() == 1
                                ? values.get(0)
                                : Arrays.toString(values.toArray())));

                return list;
            }

            public boolean add(String name, String value) {
                return super.add(new Header(name, value));
            }

            public boolean contains(String name) {
                return stream().anyMatch(it -> it.name.equals(name));
            }

            public String get(String key) {
                return stream()
                        .filter(it -> it.name.equals(key))
                        .findAny()
                        .map(Header::getValue)
                        .orElse(null);
            }

            public void forEach(BiConsumer<String, String> action) {
                forEach(header -> action.accept(header.getName(), header.getValue()));
            }
        }
    }

    public static class Response {
        private final int statusCode;
        private final String mimeType;
        private final @Nullable UniNode body;
        private final @Nullable FileHandle file;
        private final Header.List headers;

        public int getStatusCode() {
            return statusCode;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Processor<UniNode> getBody() {
            return ofConstant(body);
        }

        public Processor<FileHandle> getFile() {
            return ofConstant(file);
        }

        public Header.List getHeaders() {
            return headers;
        }

        /**
         * Creates an empty response and no extra headers.
         *
         * @param statusCode the status code
         * @see Response#Response(int, Header.List) superloaded
         */
        public Response(
                int statusCode
        ) {
            this(statusCode, new Header.List());
        }

        /**
         * Creates an empty response, using mimeType {@code *\/*} and the given extra {@code headers}.
         *
         * @param statusCode the status code
         * @param headers    the response headers
         */
        public Response(
                int statusCode,
                Header.List headers
        ) {
            this.statusCode = statusCode;
            this.mimeType = "*/*";
            this.body = null;
            this.file = null;
            this.headers = headers;
        }

        /**
         * Creates a non-empty response, using the given {@code body}.
         *
         * @param statusCode the status code
         * @param body       the response body
         * @see Response#Response(int, UniNode, Header.List) superloaded
         */
        public Response(
                int statusCode,
                UniNode body
        ) {
            this(statusCode, body, new Header.List());
        }

        /**
         * Creates a non-empty response, using the given {@code body} and the given extra {@code headers}.
         *
         * @param statusCode the status code
         * @param body       the response body
         * @param headers    the response headers
         */
        public Response(
                int statusCode,
                UniNode body,
                Header.List headers
        ) {
            this.statusCode = statusCode;
            this.body = Objects.requireNonNull(body, "body");
            this.mimeType = body.getMimeType();
            this.file = null;
            this.headers = Objects.requireNonNull(headers, "headers list");
        }

        /**
         * Creates a non-empty response, using the given {@code file} as response.
         * A {@code mimeType} parameter is required.
         *
         * @param statusCode the status code
         * @param mimeType   the mimeType of the response file
         * @param file       the file to send in response
         * @see Response#Response(int, String, File, Header.List) superloaded
         */
        public Response(
                int statusCode,
                String mimeType,
                File file
        ) {
            this(statusCode, mimeType, file, new Header.List());
        }

        /**
         * Creates a non-empty response, using the given {@code file} as response and the given extra {@code headers}.
         * This constructor tries to {@linkplain FileHandle#guessMimeTypeFromName(String) guess the mime type} of the file.
         *
         * @param statusCode the status code
         * @param file       the file to send in response
         * @param headers    the response headers
         * @see Response#Response(int, String, File, Header.List) superloaded
         */
        public Response(
                int statusCode,
                File file,
                Header.List headers
        ) {
            this(statusCode, FileHandle.guessMimeTypeFromName(file.getName()), file, headers);
        }

        /**
         * Creates a non-empty response, using the given {@code file} as response and the given extra {@code headers}.
         * A {@code mimeType} parameter is required.
         *
         * @param statusCode the status code
         * @param mimeType   the mimeType of the response file
         * @param file       the file to send in response
         * @param headers    the response headers
         */
        public Response(
                int statusCode,
                String mimeType,
                File file,
                Header.List headers
        ) {
            this.statusCode = statusCode;
            this.mimeType = Objects.requireNonNull(mimeType, "mimeType");
            this.body = null;
            this.file = FileHandle.of(Objects.requireNonNull(file, "file"));
            this.headers = Objects.requireNonNull(headers, "headers list");
        }

        /**
         * Creates any kind of response.
         *
         * @param statusCode the status code
         * @param mimeType   the mimeType of the response file
         * @param file       the file to send in response
         * @param headers    the response headers
         */
        @Internal
        public Response(
                int statusCode,
                String mimeType,
                @Nullable UniNode body,
                @Nullable File file,
                Header.List headers
        ) {
            this.statusCode = statusCode;
            this.mimeType = mimeType;
            this.body = body;
            this.file = ofConstant(file).into(FileHandle::of);
            this.headers = headers;
        }

        @Deprecated
        public Response(REST rest, int statusCode, String body) {
            this(statusCode, rest.serializationAdapter.createUniNode(body));
        }

        @Deprecated
        public static Response empty(SerializationAdapter seriLib, @MagicConstant(valuesFromClass = HTTPStatusCodes.class) int code) {
            return new Response(code, seriLib.createUniNode(null));
        }
    }

    public final class Request<T> {
        private final Header.List headers;
        private final Invocable<T> tProducer;
        private final CompletableFuture<REST.Response> execution = new CompletableFuture<>();
        private CompleteEndpoint endpoint;
        private Method method;
        private String body;
        private int expectedCode = HTTPStatusCodes.OK;

        public final CompleteEndpoint getEndpoint() {
            return endpoint;
        }

        public final Method getMethod() {
            return method;
        }

        public final String getBody() {
            return body;
        }

        public final Header.List getHeaders() {
            return headers;
        }

        public REST getREST() {
            return REST.this;
        }

        public Request(Invocable<T> tProducer) {
            this.tProducer = tProducer;
            this.headers = new Header.List();
        }

        @Override
        public String toString() {
            return String.format("%s @ %s", method.name(), endpoint.getSpec());
        }

        public Request<T> expect(@MagicConstant(valuesFromClass = HTTPStatusCodes.class) int code) {
            this.expectedCode = code;

            return this;
        }

        public Request<T> endpoint(CompleteEndpoint endpoint) {
            this.endpoint = endpoint;

            return this;
        }

        public Request<T> endpoint(AccessibleEndpoint endpoint, Object... args) {
            return endpoint(endpoint.complete(args));
        }

        public Request<T> method(REST.Method method) {
            this.method = method;

            return this;
        }

        public Request<T> body(String body) {
            this.body = body;

            return this;
        }

        public <B extends UniNode> Request<T> buildBody(BodyBuilderType<B> type, Consumer<B> bodyBuilder) {
            final B body = type.apply(serializationAdapter);
            bodyBuilder.accept(body);
            return body(body.toString());
        }

        public Request<T> addHeader(String name, String value) {
            this.headers.add(new Header(name, value));

            return this;
        }

        public boolean removeHeaders(Predicate<Header> filter) {
            return headers.removeIf(filter);
        }

        public synchronized CompletableFuture<REST.Response> execute() {
            if (!execution.isDone()) {
                logger.at(Level.FINE).log("Executing request %s @ %s");
                getREST().ratelimiter.apply(endpoint.getEndpoint(), this)
                        .thenComposeAsync(request -> httpAdapter.call(request, serializationAdapter.getMimeType()), executor)
                        .thenAcceptAsync(response -> {
                            if (response.statusCode != expectedCode) {
                                logger.at(Level.WARNING).log("Unexpected Response status code %d; expected %d", response.statusCode, expectedCode);
                            }

                            execution.complete(response);
                        }, executor);
            }

            return execution;
        }

        public CompletableFuture<Integer> execute$statusCode() {
            return execute().thenApply(Response::getStatusCode);
        }

        public CompletableFuture<UniNode> execute$body() {
            return execute()
                    .thenApply(Response::getBody)
                    .thenApply(Processor::get);
        }

        public CompletableFuture<Span<T>> execute$deserialize() {
            return execute$body().thenApply(node -> {
                switch (node.getType()) {
                    case OBJECT:
                        return Span.singleton(tProducer.autoInvoke(dependency, node.asObjectNode()));
                    case ARRAY:
                        return node.asArrayNode()
                                .asNodeList()
                                .stream()
                                .map(UniNode::asObjectNode)
                                .map(tProducer::autoInvoke)
                                .collect(Span.collector());
                    case VALUE:
                        throw new AssertionError("Cannot deserialize from UniValueNode");
                }

                throw new AssertionError();
            });
        }

        public CompletableFuture<T> execute$deserializeSingle() {
            return execute$deserialize().thenApply(Span::requireNonNull);
        }

        public <R> CompletableFuture<Span<R>> execute$map(Function<T, R> remapper) {
            return execute$deserialize().thenApply(span -> span.stream()
                    .map(remapper)
                    .collect(Span.collector()));
        }

        public <R> CompletableFuture<R> execute$mapSingle(Function<T, R> remapper) {
            return execute$deserialize().thenApply(span -> {
                if (!span.isSingle()) {
                    throw new IllegalArgumentException("Span too large");
                }

                return remapper.apply(span.get());
            });
        }

        public <ID> CompletableFuture<Span<T>> execute$autoCache(
                VarBind<?, ?, ?, ID> identifyBind, Cache<ID, T> cache
        ) {
            return execute$body().thenApply(node -> {
                if (node.isObjectNode()) {
                    return Span.singleton(cacheProduce(identifyBind, cache, node.asObjectNode()));
                } else if (node.isArrayNode()) {
                    return node.asNodeList()
                            .stream()
                            .map(UniNode::asObjectNode)
                            .map(obj -> cacheProduce(identifyBind, cache, obj))
                            .collect(Span.collector());
                } else {
                    throw new AssertionError();
                }
            });
        }

        private <ID> T cacheProduce(VarBind<?, ?, ?, ID> identifyBind, Cache<ID, T> cache, UniObjectNode obj) {
            ID id = identifyBind.getFrom(obj);

            if (id == null) {
                throw new IllegalArgumentException("Invalid Data: Could not resolve identifying Bind");
            }

            return cache.getReference(id, true)
                    .compute(old -> {
                        if (old instanceof DataContainer) {
                            Polyfill.<DataContainer<? super T>>uncheckedCast(old).updateFrom(obj);
                            return old;
                        }

                        return tProducer.autoInvoke(dependency, obj);
                    });
        }
    }
}
