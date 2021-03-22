package org.comroid.restless;

import com.sun.net.httpserver.Headers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Named;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.model.RefList;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceList;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.body.BodyBuilderType;
import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.restless.endpoint.CompleteEndpoint;
import org.comroid.restless.endpoint.RatelimitedEndpoint;
import org.comroid.restless.endpoint.TypeBoundEndpoint;
import org.comroid.restless.server.Ratelimiter;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.model.Serializable;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.comroid.varbind.container.DataContainerBase;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.*;
import java.util.stream.Collectors;

public final class REST implements ContextualProvider.Underlying {
    private static final Logger logger = LogManager.getLogger();
    private final ContextualProvider context;
    private final Ratelimiter ratelimiter;
    private final Executor executor;

    @Deprecated
    public HttpAdapter getHttpAdapter() {
        return requireFromContext(HttpAdapter.class);
    }

    @Deprecated
    public SerializationAdapter<?, ?, ?> getSerializationAdapter() {
        return requireFromContext(SerializationAdapter.class);
    }

    public Ratelimiter getRatelimiter() {
        return ratelimiter;
    }

    public final Executor getExecutor() {
        return executor;
    }

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public REST(
            ContextualProvider context
    ) {
        this(context, ForkJoinPool.commonPool());
    }

    public REST(
            ContextualProvider context,
            Executor requestExecutor
    ) {
        this(context, requestExecutor, Ratelimiter.INSTANT);
    }

    public REST(
            ContextualProvider context,
            ScheduledExecutorService scheduledExecutorService,
            RatelimitedEndpoint... pool
    ) {
        this(context, scheduledExecutorService, Ratelimiter.ofPool(scheduledExecutorService, pool));
    }

    public REST(
            ContextualProvider context,
            Executor requestExecutor,
            Ratelimiter ratelimiter
    ) {
        this.context = context.plus(this);
        this.executor = Objects.requireNonNull(requestExecutor, "RequestExecutor");
        this.ratelimiter = Objects.requireNonNull(ratelimiter, "Ratelimiter");
    }

    public Request<UniNode> request() {
        return new Request<>((context, data) -> data);
    }

    public <T extends DataContainer<? super T>> Request<T> request(Class<T> type) {
        return request(DataContainerBase.findRootBind(type));
    }

    public <T extends DataContainer<? super T>> Request<T> request(GroupBind<T> group) {
        return request(Polyfill.<BiFunction<ContextualProvider, UniNode, T>>uncheckedCast(group.getResolver()
                .orElseThrow(() -> new NoSuchElementException("No resolver applied to GroupBind " + group))));
    }

    public <T extends DataContainer<? super T>> Request<T> request(TypeBoundEndpoint<T> endpoint) {
        return request(endpoint.getBoundType()).endpoint(endpoint);
    }

    public <T> Request<T> request(BiFunction<ContextualProvider, UniNode, T> creator) {
        return new Request<>(creator);
    }

    public enum Method implements Named {
        GET,

        PUT,

        POST,

        PATCH,

        DELETE,

        HEAD;

        @Override
        public String getName() {
            return name();
        }

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

        @Override
        public String toString() {
            return String.format("{%s=%s}", name, value);
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

            @Override
            public String toString() {
                return Arrays.toString(toArray());
            }
        }
    }

    public static class Response {
        private final int statusCode;
        private final String mimeType;
        private final @Nullable Serializable body;
        private final @Nullable Reader data;
        private final Header.List headers;

        public int getStatusCode() {
            return statusCode;
        }

        public String getMimeType() {
            return mimeType;
        }

        public Reference<Serializable> getBody() {
            return Reference.constant(body);
        }

        public Reference<Reader> getData() {
            return Reference.constant(data);
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

        public Response(
                URI uri
        ) {
            this(uri, true);
        }

        public Response(
                URI uri,
                boolean isPermanent
        ) {
            this(isPermanent ? HTTPStatusCodes.PERMANENT_REDIRECT : HTTPStatusCodes.TEMPORARY_REDIRECT, redirectHeaderList(uri));
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
            this(statusCode, "*/*", null, null, headers);
        }

        /**
         * Creates a non-empty response, using the given {@code body}.
         *
         * @param statusCode the status code
         * @param body       the response body
         * @see Response#Response(int, Serializable, Header.List) superloaded
         */
        public Response(
                int statusCode,
                Serializable body
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
                Serializable body,
                Header.List headers
        ) {
            this(statusCode, body.toUniNode().getMimeType(), body, null, headers);
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
        ) throws FileNotFoundException {
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
        ) throws FileNotFoundException {
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
        ) throws FileNotFoundException {
            this(statusCode, mimeType, new InputStreamReader(new FileInputStream(file)), headers);
        }

        public Response(
                int statusCode,
                String mimeType,
                @Nullable Reader data
        ) {
            this(statusCode, mimeType, data, new Header.List());
        }

        public Response(
                int statusCode,
                String mimeType,
                @Nullable Reader data,
                Header.List headers
        ) {
            this(statusCode, mimeType, null, data, headers);
        }

        private Response(
                int statusCode,
                String mimeType,
                @Nullable Serializable body,
                @Nullable Reader data,
                Header.List headers
        ) {
            this.statusCode = statusCode;
            this.mimeType = mimeType;
            this.body = body;
            this.data = data;
            this.headers = headers;
        }

        private static Header.List redirectHeaderList(URI uri) {
            Header.List headers = new Header.List();
            headers.add(CommonHeaderNames.REDIRECT_TARGET, uri.toString());
            return headers;
        }

        @Deprecated
        public static Response empty(SerializationAdapter seriLib, @MagicConstant(valuesFromClass = HTTPStatusCodes.class) int code) {
            return new Response(code, seriLib.createUniNode(null));
        }
    }

    public final class Request<T> {
        private final Header.List headers;
        private final BiFunction<ContextualProvider, UniNode, T> tProducer;
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

        public boolean isExecuted() {
            return execution.isDone();
        }

        public Request(BiFunction<ContextualProvider, UniNode, T> tProducer) {
            this.tProducer = tProducer;
            this.headers = new Header.List();

            addHeader(CommonHeaderNames.REQUEST_CONTENT_TYPE, requireFromContext(SerializationAdapter.class).getMimeType());
            addHeader(CommonHeaderNames.ACCEPTED_CONTENT_TYPE, requireFromContext(SerializationAdapter.class).getMimeType());
        }

        @Override
        public String toString() {
            return String.format("REST.Request<%s @ %s; executed=%s>",
                    method,
                    endpoint == null ? "undefined" : endpoint.getSpec(),
                    execution.isDone());
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

        public Request<T> body(Serializable body) {
            return body(body.toSerializedString());
        }

        public Request<T> body(String body) {
            this.body = body;

            return this;
        }

        public <B extends UniNode> Request<T> buildBody(BodyBuilderType<B> type, Consumer<B> bodyBuilder) {
            final B body = type.apply(requireFromContext(SerializationAdapter.class));
            bodyBuilder.accept(body);
            return body(body.toString());
        }

        public Request<T> addHeaders(Header.List headers) {
            headers.forEach(this::addHeader);

            return this;
        }

        public Request<T> addHeader(String name, String value) {
            this.headers.add(new Header(name, value));

            return this;
        }

        public boolean removeHeaders(Predicate<Header> filter) {
            return headers.removeIf(filter);
        }

        public synchronized CompletableFuture<REST.Response> execute() {
            if (!isExecuted()) {
                addHeader("Content-Length", String.valueOf(body == null ? 0 : body.length()));
                logger.trace("Executing request {} @ {} with body {}", method, endpoint.getSpec(), String.valueOf(body));
                logger.log(Level.ALL, "Request has Headers: {}", headers.toString());
                getREST().ratelimiter.apply(endpoint.getEndpoint(), this)
                        .thenComposeAsync(request -> requireFromContext(HttpAdapter.class)
                                .call(request), executor)
                        .thenAcceptAsync(response -> {
                            if (response.statusCode != expectedCode) {
                                logger.warn("Unexpected Response status code {}; expected {}", response.statusCode, expectedCode);
                            }
                            logger.trace("{} @ {} responded with {} body {}", method,
                                    endpoint.getSpec(), response.statusCode, response.getBody().into(Objects::toString));

                            try {
                                execution.complete(response);
                            } catch (Throwable t) {
                                throw new RuntimeException("A problem occurred while handling response " + response, t);
                            }
                        }, executor);
            }

            return execution;
        }

        public CompletableFuture<Integer> execute$statusCode() {
            return execute().thenApply(Response::getStatusCode);
        }

        public CompletableFuture<Serializable> execute$body() {
            return execute()
                    .thenApply(Response::getBody)
                    .thenApply(Rewrapper::get);
        }

        public CompletableFuture<Span<T>> execute$deserialize() {
            return execute$body().thenApply(Serializable::toUniNode)
                    .thenApply(node -> {
                        switch (node.getNodeType()) {
                            case OBJECT:
                                return Span.singleton(tProducer.apply(context, node.asObjectNode()));
                            case ARRAY:
                                return node.asArrayNode()
                                        .streamNodes()
                                        .map(data -> tProducer.apply(REST.this, data))
                                        .collect(Span.collector());
                            case VALUE:
                                throw new AssertionError("Cannot deserialize from UniValueNode");
                        }

                        throw new AssertionError();
                    });
        }

        public CompletableFuture<T> execute$deserializeSingle() {
            return execute$deserialize().thenApply(span -> span.isEmpty() ? null : span.requireSingle());
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

        public <ID> CompletableFuture<RefList<T>> execute$autoCache(
                VarBind<?, ?, ?, ID> identifyBind, Cache<ID, T> cache
        ) {
            return execute$body().thenApply(Serializable::toUniNode)
                    .thenApply(node -> {
                        if (node.isObjectNode()) {
                            return ReferenceList.of(cacheProduce(identifyBind, cache, node.asObjectNode()));
                        } else if (node.isArrayNode()) {
                            return node.streamNodes()
                                    .map(UniNode::asObjectNode)
                                    .map(obj -> cacheProduce(identifyBind, cache, obj))
                                    .collect(Collectors.toCollection(ReferenceList::new));
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

                        return tProducer.apply(context, obj);
                    });
        }
    }
}
