package org.comroid.restless.socket;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Named;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.model.RefPipe;
import org.comroid.mutatio.pipe.EventPipeline;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniNode;

import java.io.Closeable;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface Websocket extends Named, Closeable, EventPipeline<WebsocketPacket.Type, WebsocketPacket> {
    URI getURI();

    @Override
    default String getName() {
        return getURI().getHost();
    }

    Executor getExecutor();

    default RefPipe<?, ?, ?, UniNode> createDataPipeline(ContextualProvider context) {
        SerializationAdapter<?, ?, ?> seriLib = context.requireFromContext(SerializationAdapter.class);
        return on(WebsocketPacket.Type.DATA)
                .flatMap(WebsocketPacket::getData)
                .map(seriLib::parse);
    }

    default CompletableFuture<Websocket> send(String data) {
        return send(data, 1024);
    }

    default CompletableFuture<Websocket> send(String data, int maxLength) {
        return send(Polyfill.splitStringForLength(data, maxLength));
    }

    CompletableFuture<Websocket> send(String[] splitMessage);

    CompletableFuture<? extends Websocket> open();
}
