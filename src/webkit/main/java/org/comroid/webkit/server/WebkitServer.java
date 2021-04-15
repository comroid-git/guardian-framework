package org.comroid.webkit.server;

import com.sun.net.httpserver.Headers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.NFunction;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.CompleteEndpoint;
import org.comroid.restless.endpoint.ScopedEndpoint;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.server.RestServer;
import org.comroid.restless.server.ServerEndpoint;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

public final class WebkitServer implements ContextualProvider.Underlying, Closeable, PagePropertiesProvider, RestEndpointException.RecoverStage {
    private static final Logger logger = LogManager.getLogger();
    private final ContextualProvider context;
    private final ScheduledExecutorService executor;
    private final String urlBase;
    private final PagePropertiesProvider pagePropertiesProvider;
    private final WebkitEndpoints endpoints;
    private final RestServer rest;
    private final WebSocketServer socket;

    public ScheduledExecutorService getExecutor() {
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
            ScheduledExecutorService executor,
            String urlBase,
            InetAddress inetAddress,
            int port,
            int socketPort,
            NFunction.In3<WebSocket, REST.Header.List, ContextualProvider, ? extends WebkitConnection> connectionConstructor,
            PagePropertiesProvider pagePropertiesProvider,
            ServerEndpoint... additionalEndpoints
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
                additionalEndpoints);
    }

    public WebkitServer(
            ContextualProvider context,
            ScheduledExecutorService executor,
            String urlBase,
            InetAddress inetAddress,
            int port,
            int socketPort,
            ConnectionFactory<? extends WebkitConnection> connectionFactory,
            PagePropertiesProvider pagePropertiesProvider,
            ServerEndpoint... additionalEndpoints
    ) throws IOException {
        context.addToContext(this);
        this.context = context;
        this.executor = executor;
        this.urlBase = urlBase;
        this.pagePropertiesProvider = pagePropertiesProvider;
        this.endpoints = new WebkitEndpoints(additionalEndpoints);
        this.rest = new RestServer(this, executor, urlBase, inetAddress, port, endpoints.getEndpoints());
        rest.setDefaultEndpoint(endpoints.defaultEndpoint);
        this.socket = new WebSocketServer(
                this.context,
                executor,
                urlBase + "/websocket",
                inetAddress,
                socketPort,
                connectionFactory
        );

        logger.info("Webkit Server available at http://{}:{}", inetAddress.getHostName(), port);
    }

    @Override
    public Map<String, Object> findPageProperties(REST.Header.List headers) {
        return Collections.unmodifiableMap(pagePropertiesProvider.findPageProperties(headers));
    }

    @Override
    public REST.Response tryRecover(
            ContextualProvider context,
            RestEndpointException exception,
            CompleteEndpoint failedEndpoint,
            int statusCode, REST.Method requestMethod,
            Headers requestHeaders,
            String[] args,
            String requestBody
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

        REST.Header.List headers = REST.Header.List.of(requestHeaders);
        Map<String, Object> pageProperties = findPageProperties(headers);
        String requestUrl = failedEndpoint.getSpec();

        final UniObjectNode errorData = this.<UniNode>findSerializer("application/json").createObjectNode().asObjectNode();
        errorData.put("requestMethod", requestMethod);
        errorData.put("requestUrl", requestUrl);
        errorData.put("statusCode", statusCode);
        errorData.put("exception", exception == null ? null
                : String.format("%s: %s", exception.getClass().getName(), exception.getMessage()));
        errorData.put("stackTrace", exceptionStackTrace);

        pageProperties.put("errorData", errorData);

        FrameBuilder frameBuilder = new FrameBuilder(headers, pageProperties, true);

        return new REST.Response(200, "text/html", frameBuilder.toReader());
    }

    @Override
    public void close() throws IOException {
        rest.close();
        socket.close();
    }

    private final class WebkitEndpoints {
        private final Map<WebkitScope, WebkitEndpoint> endpointCache = new ConcurrentHashMap<>();
        private final WebkitEndpoint defaultEndpoint = new WebkitEndpoint(WebkitScope.FRAME);
        private final Collection<ServerEndpoint> additionalEndpoints;

        public ServerEndpoint[] getEndpoints() {
            return Stream.concat(
                    endpointCache.values().stream(),
                    additionalEndpoints.stream()
            ).toArray(ServerEndpoint[]::new);
        }

        public WebkitEndpoints(ServerEndpoint[] additionalEndpoints) {
            this.additionalEndpoints = Arrays.asList(additionalEndpoints);

            forScope(WebkitScope.WEBKIT_API).assertion("API Endpoint");
        }

        public Rewrapper<WebkitEndpoint> forScope(WebkitScope scope) {
            return () -> endpointCache.computeIfAbsent(scope, WebkitEndpoint::new);
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
                requestUrl = requestUrl.substring(urlBase.length());
                return requestUrl.split("/");
            }
        }
    }
}
