package org.comroid.restless.socket;

import org.comroid.mutatio.pipe.Pipe;

import java.io.Closeable;
import java.net.URI;
import java.util.concurrent.Executor;

public interface Websocket extends Closeable {
    Pipe<? extends WebsocketPacket> getPacketPipeline();

    URI getURI();

    Executor getExecutor();
}
