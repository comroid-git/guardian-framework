package org.comroid.restless.socket;

import org.comroid.mutatio.pump.Pump;

import java.net.URI;
import java.util.concurrent.Executor;

public interface WebSocket {
    Pump<? extends WebSocketPacket> getPacketPump();

    URI getURI();

    Executor getExecutor();
}
