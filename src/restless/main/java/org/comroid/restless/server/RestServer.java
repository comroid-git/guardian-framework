package org.comroid.restless.server;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.REST.Response;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.Serializable;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.comroid.restless.CommonHeaderNames.REQUEST_CONTENT_TYPE;
import static org.comroid.restless.HTTPStatusCodes.*;

public class RestServer implements Closeable {
    private static final Response dummyResponse = new Response(0);
    private static final Logger logger = LogManager.getLogger();
    private final AutoContextHandler autoContextHandler = new AutoContextHandler();
    private final HttpServer server;
    private final Span<ServerEndpoint> endpoints;
    private final SerializationAdapter seriLib;
    private final String mimeType;
    private final String baseUrl;
    private final REST.Header.List commonHeaders = new REST.Header.List();
    private @Nullable ServerEndpoint defaultEndpoint;

    public @Nullable ServerEndpoint getDefaultEndpoint() {
        return defaultEndpoint;
    }

    public void setDefaultEndpoint(@Nullable ServerEndpoint defaultEndpoint) {
        this.defaultEndpoint = defaultEndpoint;
    }

    public HttpServer getServer() {
        return server;
    }

    public Span<ServerEndpoint> getEndpoints() {
        return endpoints;
    }

    public SerializationAdapter getSerializationAdapter() {
        return seriLib;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public REST.Header.List getCommonHeaders() {
        return commonHeaders;
    }

    public RestServer(
            ContextualProvider context,
            Executor executor,
            String baseUrl,
            InetAddress address,
            int port,
            ServerEndpoint... endpoints
    ) throws IOException {
        logger.info("Starting REST Server with {} endpoints", endpoints.length);
        this.seriLib = context.requireFromContext(SerializationAdapter.class);
        this.mimeType = seriLib.getMimeType();
        this.baseUrl = baseUrl;
        this.server = HttpServer.create(new InetSocketAddress(address, port), port);
        this.endpoints = Span.immutable(endpoints);

        server.createContext("/", autoContextHandler);
        server.setExecutor(executor);
        server.start();
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
        server.stop(5);
    }

    private void writeResponse(HttpExchange exchange, int statusCode) throws IOException {
        writeResponse(exchange, statusCode, "");
    }

    private void writeResponse(HttpExchange exchange, int statusCode, String data) throws IOException {
        exchange.sendResponseHeaders(statusCode, data.length());
        final OutputStream osr = exchange.getResponseBody();
        osr.write(data.getBytes());
        osr.flush();
    }

    private UniObjectNode generateErrorNode(RestEndpointException reex) {
        final UniObjectNode rsp = seriLib.createObjectNode();

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

    private boolean supportedMimeType(List<String> targetMimes) {
        return targetMimes.isEmpty() || targetMimes.stream()
                .anyMatch(type -> type.contains(mimeType) || type.contains("*/*"));
    }

    private class AutoContextHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            final String requestURI = baseUrl.substring(0, baseUrl.length() - 1) + exchange.getRequestURI().toString();
            final REST.Method requestMethod = REST.Method.valueOf(exchange.getRequestMethod());
            final String requestString = String.format("%s @ %s", requestMethod, requestURI);

            try {
                try {
                    final Headers responseHeaders = exchange.getResponseHeaders();
                    final Headers requestHeaders = exchange.getRequestHeaders();
                    commonHeaders.forEach(responseHeaders::add);
                    responseHeaders.add(CommonHeaderNames.ACCEPTED_CONTENT_TYPE, mimeType);
                    responseHeaders.add(REQUEST_CONTENT_TYPE, mimeType);

                    if (commonHeaders.stream().noneMatch(header -> header.getName().equals("Cookie"))
                            && requestHeaders.containsKey("Cookie"))
                        responseHeaders.add("Cookie", requestHeaders.getFirst("Cookie"));

                    logger.info("Handling {} Request @ {} with Headers: {}", requestMethod, requestURI,
                            requestHeaders
                                    .entrySet()
                                    .stream()
                                    .filter(entry -> !entry.getKey().equals(CommonHeaderNames.AUTHORIZATION))
                                    .map(entry -> String.format("%s: %s", entry.getKey(), Arrays.toString(entry.getValue().toArray())))
                                    .collect(Collectors.joining("\n- ", "\n- ", ""))
                    );

                    final String mimeType = seriLib.getMimeType();
                    final List<String> targetMimes = requestHeaders.get("Accept");
                    if (!supportedMimeType(targetMimes == null ? new ArrayList<>() : targetMimes)) {
                        logger.info(
                                "Content Type {} not supported, cancelling. Accept Header: %s",
                                mimeType,
                                targetMimes
                        );

                        throw new RestEndpointException(UNSUPPORTED_MEDIA_TYPE, String.format(
                                "Content Type %s not supported, cancelling. Accept Header: %s",
                                mimeType,
                                targetMimes
                        ));
                    }

                    String body = consumeBody(exchange);

                    logger.info("Looking for matching endpoint...");
                    forwardToEndpoint(exchange, requestURI, requestMethod, responseHeaders, requestHeaders, body);
                } catch (Throwable t) {
                    if (t instanceof RestEndpointException)
                        throw (RestEndpointException) t;
                    throw new RestEndpointException(INTERNAL_SERVER_ERROR, t);
                }
            } catch (RestEndpointException reex) {
                logger.info("An endpoint exception occurred: " + reex.getMessage(), reex);

                final String rsp = generateErrorNode(reex).toString();
                try {
                    writeResponse(exchange, reex.getStatusCode(), rsp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                exchange.close();
                logger.info("Finished handling {}", requestString);
            }
        }

        private void forwardToEndpoint(
                HttpExchange exchange,
                String requestURI,
                REST.Method requestMethod,
                Headers responseHeaders,
                Headers requestHeaders,
                String requestBody) throws Throwable {
            final Iterator<ServerEndpoint> iter = Stream.concat(
                    // endpoints that accept the request uri
                    endpoints.filter(endpoint -> endpoint.test(requestURI))
                            // handle member accessing endpoints with lower priority
                            .sorted(Comparator.comparingInt(endpoint -> endpoint.isMemberAccess(requestURI) ? 1 : -1))
                            .flatMap(ServerEndpoint.class)
                            .streamValues(),
                    // and concat to defaultEndpoint
                    Stream.of(defaultEndpoint).filter(Objects::nonNull))
                    .iterator();
            Throwable lastException = null;
            Response response = dummyResponse;

            if (!iter.hasNext()) {
                logger.info("No endpoints found; returning 404");

                throw new RestEndpointException(NOT_FOUND, "No endpoint found at URL: " + requestURI);
            }

            while (iter.hasNext()) {
                final ServerEndpoint endpoint = iter.next();

                logger.info("Attempting to use endpoint {}", endpoint.getUrlExtension());

                if (endpoint.supports(requestMethod)) {
                    final String[] args = endpoint.extractArgs(requestURI);
                    logger.info("Extracted parameters: {}", Arrays.toString(args));

                    if (args.length != endpoint.getParameterCount() && !endpoint.allowMemberAccess())
                        throw new RestEndpointException(BAD_REQUEST, "Invalid argument Count");

                    try {
                        logger.info("Executing Handler for method: {}", requestMethod);
                        response = endpoint.executeMethod(RestServer.this, requestMethod, requestHeaders, args, requestBody);
                    } catch (Throwable reex) {
                        lastException = reex;
                    }

                    if (lastException instanceof RestEndpointException)
                        throw lastException;

                    if (response == dummyResponse) {
                        logger.warn("Handler could not complete normally, attempting next handler...", lastException);
                        continue;
                    }

                    logger.info("Handler Finished! Response: {}", response);
                    handleResponse(exchange, requestURI, endpoint, responseHeaders, response);
                    lastException = null;
                    break;
                }
            }

            if (lastException != null)
                throw lastException;
        }

        private void handleResponse(
                HttpExchange exchange,
                String requestURI,
                ServerEndpoint sep,
                Headers responseHeaders,
                REST.Response response
        ) throws IOException {
            if (response == null) {
                writeResponse(exchange, OK);
                return;
            }

            response.getHeaders().forEach(responseHeaders::add);
            responseHeaders.remove(REQUEST_CONTENT_TYPE);
            responseHeaders.add(REQUEST_CONTENT_TYPE, response.getMimeType());
            final String data = unwrapData(sep, requestURI, response);

            writeResponse(exchange, response.getStatusCode(), data);

            logger.info("Sent Response code {} with length {} and Headers: {}",
                    response.getStatusCode(),
                    data.length(),
                    responseHeaders
                            .entrySet()
                            .stream()
                            .map(entry -> String.format("%s: %s", entry.getKey(), Arrays.toString(entry.getValue().toArray())))
                            .collect(Collectors.joining("\n- ", "\n- ", ""))
            );
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
    }
}
