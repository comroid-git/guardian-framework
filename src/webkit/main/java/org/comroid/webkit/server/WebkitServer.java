package org.comroid.webkit.server;

import org.comroid.api.ContextualProvider;
import org.comroid.api.UncheckedCloseable;
import org.comroid.restless.server.RestServer;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.webkit.server.WebsocketServer;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;

public class WebkitServer implements ContextualProvider.Underlying, UncheckedCloseable {
    private final ContextualProvider context;
    private final ScheduledExecutorService executor;
    private final ServerEndpoint[] additionalEndpoints;
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
            ServerEndpoint... additionalEndpoints
    ) throws IOException {
        this.context = context.plus("Webkit Server", this);
        this.executor = executor;
        this.additionalEndpoints = additionalEndpoints;
        WebkitEndpoints endpoints = new WebkitEndpoints();
        this.rest = new RestServer(this.context, executor, urlBase, inetAddress, port, endpoints.getEndpoints());
        this.socket = new WebsocketServer(this.context, executor, urlBase + "/websocket", inetAddress, port);
    }

    @Override
    public void close() {
    }

    private final class WebkitEndpoints {
        public ServerEndpoint[] getEndpoints() {
            return null;
        }
    }
}
