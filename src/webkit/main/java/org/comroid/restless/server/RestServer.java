package org.comroid.restless.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Rewrapper;
import org.comroid.api.StreamSupplier;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.REST.Response;
import org.comroid.uniform.Context;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.Serializable;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.restless.CommonHeaderNames.ACCEPTED_CONTENT_TYPE;
import static org.comroid.restless.CommonHeaderNames.REQUEST_CONTENT_TYPE;
import static org.comroid.restless.HTTPStatusCodes.*;

public final class RestServer implements HttpHandler, Closeable, Context {
    private static final Response dummyResponse = new Response(0);
    private static final Logger logger = LogManager.getLogger();
    private final Context context;
    private final HttpServer server;
    private final REST.Header.List commonHeaders = new REST.Header.List();
    private final StreamSupplier<ServerEndpoint> endpoints;
    private final Ref<ServerEndpoint> defaultEndpoint;

    public REST.Header.List getCommonHeaders() {
        return commonHeaders;
    }

    public ContextualProvider getContext() {
        return context;
    }

    public Stream<? extends ServerEndpoint> getEndpoints() {
        return endpoints.stream();
    }

    public Stream<? extends ServerEndpoint> getDefaultEndpoint() {
        return defaultEndpoint.stream();
    }

    public HttpServer getServer() {
        return server;
    }

    @Deprecated
    public RestServer(
            ContextualProvider context,
            Executor executor,
            @Nullable String baseUrl,
            InetAddress address,
            int port,
            ServerEndpoint... endpoints
    ) throws IOException {
        this(context.upgrade(Context.class), executor, new InetSocketAddress(address, port), StreamSupplier.of(endpoints));
    }

    public RestServer(
            Context context,
            Executor executor,
            InetSocketAddress socketAddress,
            StreamSupplier<ServerEndpoint> endpoints
    ) throws IOException {
        logger.info("Starting REST Server with {} endpoints", endpoints.stream().count());
        this.context = context;
        this.endpoints = endpoints;
        this.defaultEndpoint = Reference.create();
        this.server = HttpServer.create(socketAddress, socketAddress.getPort());

        server.createContext("/", this);
        server.setExecutor(executor);
        server.start();
    }

    public boolean setDefaultEndpoint(@Nullable ServerEndpoint defaultEndpoint) {
        return this.defaultEndpoint.set(defaultEndpoint);
    }

    public RestServer addCommonHeader(String name, String value) {
        this.commonHeaders.add(name, value);
        return this;
    }

    public boolean removeCommonHeader(String name) {
        return this.commonHeaders.removeIf(header -> header.getName().equals(name));
    }

    @Override
    public void close() {
        logger.info("Stopping HTTP Server");
        server.stop(5);
    }

    private void writeResponse(HttpExchange exchange, int statusCode) throws IOException {
        writeResponse(exchange, statusCode, "");
    }

    @Override
    public void handle(HttpExchange exchange) {
        logger.debug("Handling HttpExchange {}", exchange);

        try {
            // get URI and extract query parameters
            final URI uri = exchange.getRequestURI();
            String query = uri.getQuery();
            String uriStr = uri.toString();
            final String requestURI = uriStr.substring(0, uriStr.length() - (query == null ? 0 : query.length()));
            final Map<String, Object> requestQueryParameters = parseQuery(query);

            // get headers
            final REST.Method requestMethod = REST.Method.valueOf(exchange.getRequestMethod());
            final String requestString = String.format("%s @ %s", requestMethod, requestURI);
            final REST.Header.List requestHeaders = REST.Header.List.of(exchange.getRequestHeaders());

            // response vars
            String contentType = null;
            ServerEndpoint endpoint = null;
            boolean memberAccess = false;
            String[] urlParams = null;
            Response response = null;

            try {
                // get serializer for this call
                contentType = requestHeaders.getFirst(REQUEST_CONTENT_TYPE);
                final SerializationAdapter serializer = findSerializer(contentType);
                if (serializer == null)
                    throw new RestEndpointException(UNSUPPORTED_MEDIA_TYPE, "Unsupported Content-Type: " + contentType);

                logger.debug("Receiving {} {}-Request to {} with {} headers", serializer.getMimeType(), requestMethod, requestURI, requestHeaders.size());
                logger.trace("Response has headers:\n{}", requestHeaders.stream()
                        .map(REST.Header::toString)
                        .collect(Collectors.joining("\n")));

                // get request body
                String body = consumeBody(exchange);
                UniNode requestData = null;
                try {
                    requestData = serializer.parse(body);
                } catch (IllegalArgumentException e) {
                    logger.trace("Could not parse request body using selected serializer {}, attempting to parse as form data...", serializer, e);
                    requestData = serializer.createObjectNode();

                    try {
                        final UniObjectNode finalRequestData = requestData.asObjectNode();
                        Stream.of(body.split("&"))
                                .map(pair -> pair.split("="))
                                .forEach(field -> finalRequestData.put(field[0], StandardValueType.findGoodType(field[1])));
                    } catch (Throwable formParseException) {
                        logger.warn("Could not parse request body '{}'", body, formParseException);
                    }
                } finally {
                    logger.trace("Adding {} Query parameters as request body fields", requestQueryParameters.size());
                    requestData.asObjectNode().putAll(requestQueryParameters);
                }

                // find endpoint for request
                endpoint = findEndpoint(requestURI).orElseGet(defaultEndpoint);

                // validate endpoint
                if (endpoint == null)
                    throw new RestEndpointException(NOT_FOUND, "No endpoint found for request URI: " + requestURI);
                if (!endpoint.supports(requestMethod))
                    throw new RestEndpointException(METHOD_NOT_ALLOWED, "Request method not supported: " + requestMethod);
                memberAccess = endpoint.isMemberAccess(requestURI);

                // extract url parameters
                urlParams = endpoint.extractArgs(requestURI);
                urlParams = memberAccess
                        ? Arrays.copyOf(urlParams, urlParams.length - 1)
                        : urlParams;

                // execute endpoint
                response = endpoint.executeMethod(this, requestMethod, requestHeaders, urlParams, requestData);
            } catch (RestEndpointException e) {
                logger.warn("A REST Endpoint exception was thrown: {}", e.getMessage());
                try {
                    Response alternate = tryRecoverFrom(e, requestURI, INTERNAL_SERVER_ERROR, requestMethod, requestHeaders);

                    if (alternate.getStatusCode() == OK && e.getStatusCode() != OK)
                        response = alternate;
                } catch (Throwable t2) {
                    logger.debug("An error occurred during recovery", t2);
                }
            } catch (Throwable t) {
                logger.error("An error occurred during request handling", t);
                RestEndpointException wrapped = new RestEndpointException(INTERNAL_SERVER_ERROR, t);
                response = new Response(wrapped.getStatusCode(), generateErrorNode(contentType, wrapped));
            }

            // copy response headers
            final int statusCode = response.getStatusCode();
            final Headers responseHeaders = exchange.getResponseHeaders();
            response.getHeaders().forEach(responseHeaders::set);
            commonHeaders.forEach(responseHeaders::set);
            String accepted = context.getSupportedMimeTypes().collect(Collectors.joining(";"));
            responseHeaders.set(ACCEPTED_CONTENT_TYPE, accepted);

            // send response
            int r, w = 0;
            try {
                if (memberAccess) {
                    logger.debug("Attempting to write member-accessing response data");
                    Reference<Serializable> body = response.getBody();
                    UniObjectNode data = body.map(Serializable::toUniNode)
                            .map(UniNode::asObjectNode)
                            .orElseThrow(() -> new IllegalArgumentException("Invalid Data for member access: " + body.ifPresentMap(Object::toString)));
                    String yield = data.get(urlParams[urlParams.length]).toSerializedString();
                    byte[] bytes = yield.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseBody().write(bytes);
                    w = yield.length();
                } else {
                    char[] cbuf = new char[512];
                    try (
                            Reader responseData = response.getFullData();
                            OutputStreamWriter osw = new OutputStreamWriter(exchange.getResponseBody())
                    ) {
                        while ((r = responseData.read(cbuf)) != -1) {
                            osw.write(cbuf, w, r);
                            w += r;
                        }
                    }
                }
            } catch (IOException e) {
                logger.fatal("Error occurred while writing response data", e);
            } finally {
                try {
                    logger.debug("Sending Response with code {} and length {}", statusCode, w);
                    logger.trace("Response has headers:\n{}", responseHeaders.entrySet()
                            .stream()
                            .map(header -> String.format("%s = %s", header.getKey(), String.join(";", header.getValue())))
                            .collect(Collectors.joining("\n")));

                    exchange.getResponseBody().flush();
                    exchange.sendResponseHeaders(statusCode, w);
                } catch (IOException e) {
                    logger.fatal("Error occurred while sending response; cannot continue", e);
                } finally {
                    exchange.close();
                }
            }
        } catch (Throwable t) {
            logger.fatal("An error occurred during handler; cannot continue", t);
        }
    }

    private Optional<ServerEndpoint> findEndpoint(String requestURI) {
        return getEndpoints()
                .filter(endpoint -> endpoint.test(requestURI))
                .filter(endpoint -> !endpoint.allowMemberAccess() || endpoint.isMemberAccess(requestURI))
                .findFirst()
                .map(ServerEndpoint.class::cast);
    }

    private Map<String, Object> parseQuery(@Nullable String query) {
        if (query == null)
            return new HashMap<>();
        Map<String, Object> yield = new HashMap<>();

        // strip leading ? if present
        if (query.startsWith("?"))
            query = query.substring(1);

        try (
                Scanner scanner = new Scanner(query)
        ) {
            scanner.useDelimiter("&");

            while (scanner.hasNext()) {
                String[] pair = scanner.next().split("=");
                yield.put(pair[0], StandardValueType.findGoodType(pair[1]));
            }
        }
        return yield;
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String data) throws IOException {
        exchange.sendResponseHeaders(statusCode, data.length());
        final OutputStream osr = exchange.getResponseBody();
        osr.write(data.getBytes());
        osr.flush();
    }

    private UniObjectNode generateErrorNode(CharSequence mimeType, RestEndpointException reex) {
        final UniObjectNode rsp = createObjectNode(mimeType);

        rsp.put("code", StandardValueType.INTEGER, reex.getStatusCode());
        rsp.put("description", StandardValueType.STRING, HTTPStatusCodes.toString(reex.getStatusCode()));
        rsp.put("message", StandardValueType.STRING, reex.getSimpleMessage());

        final Throwable cause = reex.getCause();
        if (cause != null)
            rsp.put("cause", StandardValueType.STRING, cause.toString());

        return rsp;
    }

    private String consumeBody(HttpExchange exchange) {
        String str = null;

        try (
                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody());
                BufferedReader br = new BufferedReader(isr)
        ) {
            str = br.lines().collect(Collectors.joining());
        } catch (Throwable t) {
            logger.error("Could not read response");
        }

        return str;
    }

    private @Nullable Response tryRecoverFrom(
            RestEndpointException lastException,
            String requestURI,
            int statusCode,
            REST.Method requestMethod,
            REST.Header.List requestHeaders
    ) {
        logger.debug("Trying to recover from {} gotten from {} @ {}", lastException, requestMethod, requestURI);
        Rewrapper<RestEndpointException.RecoverStage> recoverStageRef = context.getFromContext(RestEndpointException.RecoverStage.class, true);

        if (recoverStageRef.isNull())
            return null;

        RestEndpointException.RecoverStage recoverStage = recoverStageRef.assertion();
        return recoverStage.tryRecover(context, lastException, requestURI, statusCode, requestMethod, requestHeaders);
    }

    private String unwrapData(ServerEndpoint sep, String requestURI, Response response) {
        return response.getBody()
                .map(Serializable::toUniNode)
                .map(responseBody -> {
                    if (responseBody == null)
                        return "";
                    if (!sep.allowMemberAccess() || !sep.isMemberAccess(requestURI))
                        return responseBody.toString();

                    String fractalName = requestURI.substring(requestURI.lastIndexOf("/") + 1);

                    if (fractalName.matches("\\d+")) {
                        // numeric fractal
                        final int fractalNum = Integer.parseInt(fractalName);

                        if (!responseBody.has(fractalNum))
                            fractalName = null;

                        if (fractalName != null)
                            return responseBody.get(fractalNum).toString();
                    } else {
                        // string fractal
                        if (!responseBody.has(fractalName))
                            fractalName = null;

                        if (fractalName != null)
                            return responseBody.get(fractalName).toString();
                    }

                    return responseBody.toString();
                })
                .or(() -> response.getData().map(r -> {
                    try (
                            BufferedReader br = new BufferedReader(r)
                    ) {
                        return br.lines().collect(Collectors.joining("\r"));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }).get())
                .orElse("");
    }

    @Override
    public Stream<Object> streamContextMembers(boolean includeChildren) {
        return context.streamContextMembers(includeChildren);
    }
}
