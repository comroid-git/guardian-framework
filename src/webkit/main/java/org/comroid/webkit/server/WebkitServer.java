package org.comroid.webkit.server;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Rewrapper;
import org.comroid.restless.endpoint.ScopedEndpoint;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.restless.server.RestServer;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.webkit.endpoint.WebkitScope;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Stream;

public class WebkitServer implements ContextualProvider.Underlying, Closeable {
    public static final String RESOURCE_PREFIX = "org.comroid.webkit/";
    public static final String INTERNAL_RESOURCE_PREFIX = "org.comroid.webkit.internal/";
    private final ContextualProvider context;
    private final ScheduledExecutorService executor;
    private final String urlBase;
    private final WebkitEndpoints endpoints;
    private final RestServer rest;
    private final WebsocketServer socket;

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

    public WebsocketServer getSocket() {
        return socket;
    }

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public WebkitServer(
            ContextualProvider context,
            ScheduledExecutorService executor,
            String urlBase,
            InetAddress inetAddress,
            int port,
            int socketPort,
            ServerEndpoint... additionalEndpoints
    ) throws IOException {
        this.context = context.plus("Webkit Server", this);
        this.executor = executor;
        this.urlBase = urlBase;
        this.endpoints = new WebkitEndpoints(additionalEndpoints);
        this.rest = new RestServer(this.context, executor, urlBase, inetAddress, port, endpoints.getEndpoints());
        rest.setDefaultEndpoint(endpoints.defaultEndpoint);
        this.socket = new WebsocketServer(this.context, executor, urlBase + "/websocket", inetAddress, socketPort);
    }

    @Override
    public void close() throws IOException {
        rest.close();
        socket.close();
    }

    private final class WebkitEndpoints {
        private final Map<WebkitScope, WebkitEndpoint> endpointCache = new ConcurrentHashMap<>();
        private final WebkitEndpoint defaultEndpoint = new WebkitEndpoint(WebkitScope.DEFAULT);
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

        private class WebkitEndpoint extends ScopedEndpoint implements ServerEndpoint.This {
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
                if (scope != WebkitScope.DEFAULT || requestUrl.indexOf(urlBase) != 0)
                    return super.extractArgs(requestUrl);
                // is Default Endpoint
                requestUrl = requestUrl.substring(urlBase.length());
                return requestUrl.split("/");
            }
        }
    }
}
