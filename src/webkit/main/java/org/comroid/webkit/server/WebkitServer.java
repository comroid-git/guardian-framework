package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.NFunction;
import org.comroid.api.Rewrapper;
import org.comroid.api.StreamSupplier;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.restless.MimeType;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.ScopedEndpoint;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.restless.server.RestServer;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.webkit.endpoint.WebkitScope;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.comroid.webkit.socket.ConnectionFactory;
import org.comroid.webkit.socket.WebkitConnection;
import org.java_websocket.WebSocket;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.stream.Stream;

public final class WebkitServer implements ContextualProvider.Underlying, Closeable, PagePropertiesProvider, RestEndpointException.RecoverStage {
    private static final Logger logger = LogManager.getLogger();
    private final Context context;
    private final Executor executor;
    private final String urlBase;
    private final PagePropertiesProvider pagePropertiesProvider;
    private final WebkitEndpoints endpoints;
    private final RestServer rest;
    private final WebSocketServer socket;

    public Executor getExecutor() {
        return executor;
    }

    public String getUrlBase() {
        return urlBase;
    }

    public WebkitEndpoints getEndpoints() {
        return endpoints;
    }

    public RestServer getRest() {
        return rest;
    }

    public WebSocketServer getSocket() {
        return socket;
    }

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public RefContainer<?, WebkitConnection> getActiveConnections() {
        return socket.getActiveConnections().flatMap(WebkitConnection.class);
    }

    public WebkitServer(
            ContextualProvider context,
            Executor executor,
            String urlBase,
            InetAddress inetAddress,
            int port,
            int socketPort,
            NFunction.In3<WebSocket, REST.Header.List, ContextualProvider, ? extends WebkitConnection> connectionConstructor,
            PagePropertiesProvider pagePropertiesProvider,
            StreamSupplier<ServerEndpoint> additionalEndpoints
    ) throws IOException {
        this(
                context,
                executor,
                urlBase,
                inetAddress,
                port,
                socketPort,
                new ConnectionFactory<>(connectionConstructor, context),
                pagePropertiesProvider,
                additionalEndpoints
        );
    }

    public WebkitServer(
            ContextualProvider context,
            Executor executor,
            String urlBase,
            InetAddress inetAddress,
            int port,
            int socketPort,
            ConnectionFactory<? extends WebkitConnection> connectionFactory,
            PagePropertiesProvider pagePropertiesProvider,
            StreamSupplier<ServerEndpoint> additionalEndpoints
    ) throws IOException {
        context.addToContext(this);
        this.context = context.upgrade(Context.class);
        this.executor = executor;
        this.urlBase = urlBase;
        this.pagePropertiesProvider = pagePropertiesProvider;
        this.endpoints = new WebkitEndpoints();
        this.rest = new RestServer(
                this.context,
                executor,
                new InetSocketAddress(inetAddress, port),
                additionalEndpoints.append(endpoints));
        rest.setDefaultEndpoint(endpoints.defaultEndpoint);
        this.socket = new WebSocketServer(
                this.context,
                executor,
                urlBase + "/websocket",
                inetAddress,
                socketPort,
                connectionFactory
        );
    }

    @Override
    public Map<String, Object> findPageProperties(REST.Header.List headers) {
        return pagePropertiesProvider.findPageProperties(headers);
    }

    @Override
    public REST.Response tryRecover(
            ContextualProvider context,
            Throwable exception,
            String requestURI,
            int statusCode,
            REST.Method requestMethod,
            REST.Header.List headers
    ) {
        String exceptionStackTrace = null;
        if (exception != null) {
            try (
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter)
            ) {
                exception.printStackTrace(printWriter);
                exceptionStackTrace = stringWriter.toString();
            } catch (IOException e) {
                logger.error("Recovering - Error when reading StackTrace: ", e);
            }
        }

        Map<String, Object> pageProperties = findPageProperties(headers);

        final UniObjectNode errorData = this.<UniNode>findSerializer(MimeType.JSON).createObjectNode().asObjectNode();
        errorData.put("requestMethod", requestMethod);
        errorData.put("requestUrl", requestURI);
        errorData.put("statusCode", statusCode);
        errorData.put("exception", exception == null ? null
                : String.format("%s: %s", exception.getClass().getName(), exception.getMessage()));
        errorData.put("stackTrace", exceptionStackTrace);

        pageProperties.put("errorData", errorData);

        FrameBuilder frameBuilder = new FrameBuilder("main", headers, pageProperties, true);

        return new REST.Response(200, "text/html", frameBuilder.toReader());
    }

    @Override
    public void close() throws IOException {
        rest.close();
        socket.close();
    }

    private final class WebkitEndpoints implements StreamSupplier<ServerEndpoint> {
        private final Map<WebkitScope, WebkitEndpoint> endpointCache = new ConcurrentHashMap<>();
        private final WebkitEndpoint defaultEndpoint = new WebkitEndpoint(WebkitScope.FRAME);

        public WebkitEndpoints() {
            forScope(WebkitScope.WEBKIT_API).assertion("API Endpoint");
        }

        public Rewrapper<WebkitEndpoint> forScope(WebkitScope scope) {
            return () -> endpointCache.computeIfAbsent(scope, WebkitEndpoint::new);
        }

        @Override
        public Stream<? extends ServerEndpoint> stream() {
            return endpointCache.values().stream();
        }

        private class WebkitEndpoint extends ScopedEndpoint implements ServerEndpoint.This, EndpointHandler.Underlying {
            private final WebkitScope scope;

            @Override
            public EndpointHandler getEndpointHandler() {
                return scope;
            }

            private WebkitEndpoint(WebkitScope scope) {
                super(scope, urlBase);

                this.scope = scope;
            }

            @Override
            public String[] extractArgs(String requestUrl) {
                if (this != defaultEndpoint)
                    return super.extractArgs(requestUrl);
                // is Default Endpoint
                String[] split = requestUrl.split("/");
                if (split.length <= 1)
                    return new String[0];
                if (requestUrl.startsWith("/")) {
                    String[] tmp = new String[split.length - 1];
                    System.arraycopy(split, 1, tmp, 0, tmp.length);
                    split = tmp;
                }
                return split;
            }

            @Override
            public String toString() {
                return String.format("WebkitEndpoint{scope=%s}", scope);
            }
        }
    }
}
