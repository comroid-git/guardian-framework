package org.comroid.restless.server;

import com.google.common.flogger.FluentLogger;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.comroid.common.io.FileHandle;
import org.comroid.mutatio.span.Span;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.REST.Response;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.google.common.flogger.LazyArgs.lazy;
import static org.comroid.restless.HTTPStatusCodes.*;

public class RestServer implements Closeable {
    private static final Response dummyResponse = new Response(0);
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final AutoContextHandler autoContextHandler = new AutoContextHandler();
    private final HttpServer server;
    private final Span<ServerEndpoint> endpoints;
    private final SerializationAdapter seriLib;
    private final String mimeType;
    private final String baseUrl;
    private final REST.Header.List commonHeaders = new REST.Header.List();

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
            SerializationAdapter seriLib,
            Executor executor,
            String baseUrl,
            InetAddress address,
            int port,
            ServerEndpoint... endpoints
    ) throws IOException {
        logger.at(Level.INFO).log("Starting REST Server with %d endpoints", endpoints.length);
        this.seriLib = seriLib;
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
        final UniObjectNode rsp = seriLib.createUniObjectNode();

        rsp.put("code", ValueType.INTEGER, reex.getStatusCode());
        rsp.put("description", ValueType.STRING, HTTPStatusCodes.toString(reex.getStatusCode()));
        rsp.put("message", ValueType.STRING, reex.getSimpleMessage());

        final Throwable cause = reex.getCause();
        if (cause != null)
            rsp.put("cause", ValueType.STRING, cause.toString());

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
            logger.at(Level.SEVERE).log("Could not read response");
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
                    responseHeaders.add(CommonHeaderNames.REQUEST_CONTENT_TYPE, mimeType);

                    if (commonHeaders.stream().noneMatch(header -> header.getName().equals("Cookie"))
                            && requestHeaders.containsKey("Cookie"))
                        responseHeaders.add("Cookie", requestHeaders.getFirst("Cookie"));

                    logger.at(Level.INFO).log("Handling %s Request @ %s with Headers: %s", requestMethod, requestURI,
                            lazy(() -> requestHeaders
                                    .entrySet()
                                    .stream()
                                    .filter(entry -> !entry.getKey().equals(CommonHeaderNames.AUTHORIZATION))
                                    .map(entry -> String.format("%s: %s", entry.getKey(), Arrays.toString(entry.getValue().toArray())))
                                    .collect(Collectors.joining("\n- ", "\n- ", ""))
                            )
                    );

                    final String mimeType = seriLib.getMimeType();
                    final List<String> targetMimes = requestHeaders.get("Accept");
                    if (!supportedMimeType(targetMimes == null ? new ArrayList<>() : targetMimes)) {
                        logger.at(Level.INFO).log(
                                "Content Type %s not supported, cancelling. Accept Header: %s",
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

                    logger.at(Level.INFO).log("Looking for matching endpoint...");
                    forwardToEndpoint(exchange, requestURI, requestMethod, responseHeaders, requestHeaders, body);
                } catch (Throwable t) {
                    if (t instanceof RestEndpointException)
                        throw (RestEndpointException) t;
                    throw new RestEndpointException(INTERNAL_SERVER_ERROR, t);
                }
            } catch (RestEndpointException reex) {
                logger.at(Level.INFO)
                        .withCause(reex)
                        .log("An endpoint exception occurred: " + reex.getMessage());

                final String rsp = generateErrorNode(reex).toString();
                try {
                    writeResponse(exchange, reex.getStatusCode(), rsp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } finally {
                exchange.close();
                logger.at(Level.INFO).log("Finished handling %s", requestString);
            }
        }

        private void forwardToEndpoint(
                HttpExchange exchange,
                String requestURI,
                REST.Method requestMethod,
                Headers responseHeaders,
                Headers requestHeaders,
                String requestBody) throws Throwable {
            final Iterator<ServerEndpoint> iter = endpoints.pipe()
                    // endpoints that accept the request uri
                    .filter(endpoint -> endpoint.test(requestURI))
                    // handle member accessing endpoints with lower priority
                    .sorted(Comparator.comparingInt(endpoint -> endpoint.isMemberAccess(requestURI) ? 1 : -1))
                    .span()
                    .iterator();
            Throwable lastException = null;
            Response response = dummyResponse;

            if (!iter.hasNext()) {
                logger.at(Level.INFO).log("No endpoints found; returning 404");

                throw new RestEndpointException(NOT_FOUND, "No endpoint found at URL: " + requestURI);
            }

            while (iter.hasNext()) {
                final ServerEndpoint endpoint = iter.next();

                logger.at(Level.INFO).log("Attempting to use endpoint %s", endpoint.getUrlExtension());

                if (endpoint.supports(requestMethod)) {
                    final String[] args = endpoint.extractArgs(requestURI);
                    logger.at(Level.INFO).log("Extracted parameters: %s", lazy(() -> Arrays.toString(args)));

                    if (args.length != endpoint.getParameterCount() && !endpoint.allowMemberAccess())
                        throw new RestEndpointException(BAD_REQUEST, "Invalid argument Count");

                    try {
                        logger.at(Level.INFO).log("Executing Handler for method: %s", requestMethod);
                        response = endpoint.executeMethod(RestServer.this, requestMethod, requestHeaders, args, requestBody);
                    } catch (Throwable reex) {
                        lastException = reex;
                    }

                    if (lastException instanceof RestEndpointException)
                        throw lastException;

                    if (response == dummyResponse) {
                        logger.at(Level.WARNING)
                                .withCause(lastException)
                                .log("Handler could not complete normally, attempting next handler...", response);
                        continue;
                    }

                    logger.at(Level.INFO).log("Handler Finished! Response: %s", response);
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
            final String data = unwrapData(sep, requestURI, response);

            writeResponse(exchange, response.getStatusCode(), data);

            logger.at(Level.INFO).log("Sent Response code %d with length %d and Headers: %s",
                    response.getStatusCode(),
                    data.length(),
                    lazy(() -> responseHeaders
                            .entrySet()
                            .stream()
                            .map(entry -> String.format("%s: %s", entry.getKey(), Arrays.toString(entry.getValue().toArray())))
                            .collect(Collectors.joining("\n- ", "\n- ", ""))
                    )
            );
        }

        private String unwrapData(ServerEndpoint sep, String requestURI, Response response) {
            return response.getBody()
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
                    .or(() -> response.getFile().map(FileHandle::getContent).get())
                    .orElse("");
        }
    }
}
