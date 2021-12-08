package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.*;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.restless.MimeType;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.ScopedEndpoint;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.webkit.config.WebkitResourceLoader;
import org.comroid.webkit.endpoint.WebkitScope;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.ConnectionFactory;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.comroid.webkit.socket.ConnectionFactoryBase;
import org.comroid.webkit.socket.WebkitConnection;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public final class WebkitServer implements ContextualProvider.Underlying, Closeable, PagePropertiesProvider, ResourceLoader, RestEndpointException.RecoverStage {
    private static final Logger logger = LogManager.getLogger();
    private final Context context;
    private final Executor executor;
    private final String urlBase;
    private final PagePropertiesProvider pagePropertiesProvider;
    private final ResourceLoader resourceLoader;
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

    public String getSocketHost() {
        InetSocketAddress address = socket.getAddress();
        return address.getAddress().getHostAddress() + ':' + address.getPort() + "/websocket";
    }

    @Deprecated
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
                new ConnectionFactoryBase<>(connectionConstructor, context),
                pagePropertiesProvider,
                additionalEndpoints
        );
    }

    @Deprecated
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
        logger.warn("Deprecated Constructor used");
        context.addToContext(this);
        this.context = context.upgrade(Context.class);
        this.executor = executor;
        this.urlBase = urlBase;
        this.pagePropertiesProvider = pagePropertiesProvider;
        this.resourceLoader = ResourceLoader.SYSTEM_CLASS_LOADER;
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
                inetAddress,
                socketPort,
                connectionFactory
        );
    }

    public WebkitServer(
            Context context,
            String urlBase,
            int httpPort,
            int wsPort,
            @Nullable StreamSupplier<? extends ServerEndpoint> additionalEndpoints
    ) throws IOException {
        this(context, urlBase, formAddress(context, httpPort), formAddress(context, wsPort), additionalEndpoints);
    }

    public WebkitServer(
            Context context,
            String urlBase,
            InetSocketAddress httpAddress,
            InetSocketAddress wsAddress,
            @Nullable StreamSupplier<? extends ServerEndpoint> additionalEndpoints
    ) throws IOException {
        this(
                context,
                urlBase,
                httpAddress,
                wsAddress,
                context.getFromContext(Executor.class)
                        .orElseGet(ForkJoinPool::commonPool),
                context.requireFromContext(PagePropertiesProvider.class),
                context.getFromContext(ResourceLoader.class)
                        .orElseGet(() -> ResourceLoader.SYSTEM_CLASS_LOADER),
                additionalEndpoints
        );
    }

    public WebkitServer(
            Context context,
            String urlBase,
            InetSocketAddress httpAddress,
            InetSocketAddress wsAddress,
            Executor executor,
            PagePropertiesProvider pagePropertiesProvider,
            ResourceLoader resourceLoader,
            @Nullable StreamSupplier<? extends ServerEndpoint> additionalEndpoints
    ) throws IOException {
        context.addToContext(this);
        this.context = context;
        this.executor = executor;
        this.urlBase = urlBase;
        this.pagePropertiesProvider = pagePropertiesProvider;
        this.resourceLoader = WebkitResourceLoader.initialize(resourceLoader);
        this.endpoints = new WebkitEndpoints();
        this.rest = new RestServer(this, httpAddress, additionalEndpoints == null ? endpoints : endpoints.append(additionalEndpoints));
        this.rest.setDefaultEndpoint(endpoints.defaultEndpoint);
        this.socket = new WebSocketServer(this, wsAddress);
    }

    private static InetSocketAddress formAddress(ContextualProvider context, int port) {
        return context.getFromContext(InetAddress.class)
                .or(() -> context.getFromContext(InetSocketAddress.class).ifPresentMap(InetSocketAddress::getAddress))
                .or(ThrowingSupplier.rethrowing(InetAddress::getLocalHost, t -> new RuntimeException("Could not obtain localhost", t)))
                .ifPresentMapOrElseThrow(
                        address -> new InetSocketAddress(address, port),
                        () -> new NoSuchElementException("No INetAddress candidate found at context " + context.getName())
                );
    }

    @Override
    public Map<String, Object> findPageProperties(REST.Header.List headers) {
        Map<String, Object> map = pagePropertiesProvider.findPageProperties(headers);
        map.put("wsHost", getSocketHost());
        return map;
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

        FrameBuilder frameBuilder = new FrameBuilder(context, "main", headers, true);

        return new REST.Response(200, "text/html", frameBuilder.toReader());
    }

    @Override
    public void close() throws IOException {
        rest.close();
        socket.close();
    }

    @Override
    public InputStream getResource(String name) {
        return resourceLoader.getResource(name);
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
