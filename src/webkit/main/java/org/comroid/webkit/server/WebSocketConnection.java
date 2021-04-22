package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.api.PropertiesHolder;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.RefMap;
import org.comroid.mutatio.model.RefPipe;
import org.comroid.mutatio.pipe.EventPipeline;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;
import org.comroid.mutatio.ref.ReferencePipe;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebSocketClientSpec;
import org.comroid.restless.socket.WebsocketPacket;
import org.java_websocket.WebSocket;

import java.util.concurrent.Executor;

public class WebSocketConnection implements
        WebSocketClientSpec.Complete,
        ContextualProvider.Underlying,
        EventPipeline<WebsocketPacket.Type, WebsocketPacket>,
        PropertiesHolder {
    private static final Logger logger = LogManager.getLogger();
    public final RefMap<String, Object> properties = new ReferenceMap<>();
    protected final RefPipe<WebsocketPacket.Type, WebsocketPacket, WebsocketPacket.Type, WebsocketPacket> packetPipeline;
    private final WebSocket socketBase;
    private final REST.Header.List headers;
    private final ContextualProvider context;

    @Override
    public final RefContainer<WebsocketPacket.Type, WebsocketPacket> getEventPipeline() {
        return packetPipeline;
    }

    public final REST.Header.List getHeaders() {
        return headers;
    }

    @Override
    public final boolean isOpen() {
        return !(socketBase.isClosed() || socketBase.isClosing());
    }

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    protected WebSocketConnection(WebSocket socketBase, REST.Header.List headers, ContextualProvider context) {
        this.socketBase = socketBase;
        this.headers = headers;
        this.context = context;
        this.packetPipeline = new ReferencePipe<>(context.<Executor>requireFromContext(Executor.class));
    }

    @Override
    public final <T> Reference<T> getProperty(String name) {
        if (properties.containsKey(name))
            return properties.getReference(name, false)
                    .map(Polyfill::uncheckedCast);
        return Reference.empty();
    }

    @Override
    public final boolean setProperty(String name, Object value) {
        return properties.getReference(name, true).set(value);
    }

    @Override
    public final void close(int code, String reason) {
        socketBase.close(code, reason);
    }

    public final void sendText(final String payload) {
        socketBase.send(payload);
    }

    @Override
    public final void sendBinary(byte[] bytes) {
        socketBase.send(bytes);
    }

    public final void sendPing() {
        socketBase.sendPing();
    }

    void nowClosed(int code, String reason) {
        publishPacket(WebsocketPacket.close(code, reason));
    }

    void publishPacket(WebsocketPacket packet) {
        packetPipeline.accept(packet.getType(), packet);
    }
}
