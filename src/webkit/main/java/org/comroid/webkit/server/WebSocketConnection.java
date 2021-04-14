package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.RefPipe;
import org.comroid.mutatio.ref.ReferencePipe;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.webkit.model.ConnectionClientSpec;
import org.java_websocket.WebSocket;

import java.util.concurrent.Executor;

public class WebSocketConnection implements ConnectionClientSpec.Complete {
    private static final Logger logger = LogManager.getLogger();
    protected final RefPipe<WebsocketPacket.Type, WebsocketPacket, WebsocketPacket.Type, WebsocketPacket> packetPipeline;
    private final WebSocket socketBase;
    private final REST.Header.List headers;
    private final Executor executor;

    public final RefContainer<WebsocketPacket.Type, WebsocketPacket> getPacketPipeline() {
        return packetPipeline;
    }

    public final REST.Header.List getHeaders() {
        return headers;
    }

    public final Executor getExecutor() {
        return executor;
    }

    @Override
    public final boolean isOpen() {
        return !(socketBase.isClosed() || socketBase.isClosing());
    }

    public WebSocketConnection(WebSocket socketBase, REST.Header.List headers, Executor executor) {
        this.socketBase = socketBase;
        this.headers = headers;
        this.executor = executor;
        this.packetPipeline = new ReferencePipe<>(executor);
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
