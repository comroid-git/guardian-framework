package org.comroid.webkit;

import org.comroid.api.ContextualProvider;
import org.comroid.restless.server.RestServer;
import org.comroid.restless.server.ServerEndpoint;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;

public final class WebkitServer implements ContextualProvider.Underlying {
    private final ContextualProvider context;
    private final ScheduledExecutorService executor;
    private final ServerEndpoint[] additionalEndpoints;
    private final RestServer rest;

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
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
    }

    private final class WebkitEndpoints {
        public ServerEndpoint[] getEndpoints() {
            return null;
        }
    }
}
