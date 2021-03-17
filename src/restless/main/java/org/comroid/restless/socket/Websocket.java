package org.comroid.restless.socket;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Named;
import org.comroid.mutatio.model.RefPipe;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniNode;

import java.io.Closeable;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public interface Websocket extends Named, Closeable {
    RefPipe<?, ?, WebsocketPacket.Type, ? extends WebsocketPacket> getPacketPipeline();

    URI getURI();

    @Override
    default String getName() {
        return getURI().getHost();
    }

    Executor getExecutor();

    default RefPipe<?, ?, ?, UniNode> createDataPipeline(ContextualProvider context) {
        SerializationAdapter<?, ?, ?> seriLib = context.requireFromContext(SerializationAdapter.class);
        return getPacketPipeline()
                .filterKey(type -> type == WebsocketPacket.Type.DATA)
                .flatMap(WebsocketPacket::getData)
                .map(seriLib::parse);
    }

    default CompletableFuture<Websocket> send(String data) {
        return send(data, 1024);
    }

    default CompletableFuture<Websocket> send(String data, int maxLength) {
        String[] parts = new String[(data.length() / maxLength) + 1];

        for (int i = 0, end; i < parts.length; i++)
            parts[i] = data.substring(maxLength * i, (end = maxLength * (i + 1)) > data.length() ? data.length() : end);
        return send(parts);
    }

    CompletableFuture<Websocket> send(String[] splitMessage);
}
