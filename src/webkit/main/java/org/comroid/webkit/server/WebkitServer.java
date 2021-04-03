package org.comroid.webkit.server;

import org.comroid.api.ContextualProvider;
import org.comroid.api.UncheckedCloseable;
import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.restless.endpoint.EndpointScope;
import org.comroid.restless.endpoint.ScopedEndpoint;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.restless.server.RestServer;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.webkit.endpoint.WebkitScope;
import org.comroid.webkit.server.WebsocketServer;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;

public class WebkitServer implements ContextualProvider.Underlying, Closeable {
    public static final String RESOURCE_PREFIX = "org.comroid.webkit/";
    public static final String INTERNAL_RESOURCE_PREFIX = "org.comroid.webkit.internal/";
    private final ContextualProvider context;
    private final ScheduledExecutorService executor;
    private final String urlBase;
    private final WebkitEndpoints endpoints;
    private final RestServer rest;
    private final WebsocketServer socket;

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
        this.socket = new WebsocketServer(this.context, executor, urlBase + "/websocket", inetAddress, socketPort);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    private final class WebkitEndpoints {
        private final HashSet<ServerEndpoint> endpoints;

        public ServerEndpoint[] getEndpoints() {
            return endpoints.toArray(new ServerEndpoint[0]);
        }

        public WebkitEndpoints(ServerEndpoint[] additionalEndpoints) {
            this.endpoints = new HashSet<>();

            endpoints.add(new WebkitEndpoint(WebkitScope.WEBKIT_API));
            endpoints.addAll(Arrays.asList(additionalEndpoints));
        }

        private class WebkitEndpoint extends ScopedEndpoint implements ServerEndpoint.This {
            private final WebkitScope scope;

            public WebkitEndpoint(WebkitScope scope) {
                super(scope, urlBase);

                this.scope = scope;
            }

            @Override
            public EndpointHandler getEndpointHandler() {
                return scope;
            }
        }
    }
}
