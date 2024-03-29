package org.comroid.varbind;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Junction;
import org.comroid.api.Polyfill;
import org.comroid.api.Disposable;
import org.comroid.api.io.FileHandle;
import org.comroid.api.io.FileProcessor;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.function.BiFunction;

public class FileCache<K, V extends DataContainer<V>>
        extends DataContainerCache<K, V>
        implements FileProcessor, Disposable {
    private static final Logger logger = LogManager.getLogger();
    private final SerializationAdapter<?, ?, ?> seriLib;
    private final FileHandle file;
    private final UUID uuid = UUID.randomUUID();
    private final BiFunction<ContextualProvider, UniObjectNode, V> resolver;

    @Override
    public FileHandle getFile() {
        return file;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    public FileCache(
            ContextualProvider context,
            BiFunction<ContextualProvider, UniObjectNode, V> resolver,
            VarBind<? super V, K, ?, K> idBind,
            FileHandle file,
            int largeThreshold
    ) {
        this(context, resolver, idBind, null, file, largeThreshold, false);
    }

    public FileCache(
            ContextualProvider context,
            BiFunction<ContextualProvider, UniObjectNode, V> resolver,
            VarBind<? super V, K, ?, K> idBind,
            Junction<K, String> converter,
            FileHandle file,
            int largeThreshold,
            boolean keyCaching
    ) {
        super(context, largeThreshold, idBind);

        this.seriLib = context.requireFromContext(SerializationAdapter.class);
        this.resolver = resolver;
        this.file = file;
    }

    @Override
    public synchronized int storeData() throws IOException {
        final UniArrayNode data = seriLib.createArrayNode();

        streamRefs()
                .filter(ref -> !ref.isNull())
                .forEach(ref -> {
                    final V it = ref.get();

                    if (it == null) {
                        data.addNull();
                        return;
                    }

                    it.toObjectNode(data.addObject());
                });

        try (
                final FileWriter writer = new FileWriter(file, false)
        ) {
            writer.write(data.toString());
        }

        return data.size();
    }

    @Override
    public synchronized int reloadData() {
        UniArrayNode data;

        final String str = file.getContent();

        if (str.isEmpty())
            return 0;

        final UniNode uniNode = seriLib.createUniNode(str);

        if (uniNode == null || uniNode.isNull())
            return 0;
        if (!uniNode.isArrayNode())
            throw new IllegalArgumentException("Data is not an array");

        data = uniNode.asArrayNode();
        data.streamNodes()
                .filter(UniNode::isNonEmpty)
                .filter(UniNode::isObjectNode)
                .map(UniNode::asObjectNode)
                .forEach(node -> {
                    final K id = idBind.getFrom(node);

                    if (containsKey(id)) {
                        getReference(id, false).requireNonNull().updateFrom(node);
                    } else {
                        final Object generated = resolver.apply(this, node);

                        if (generated == null) {
                            logger.warn("Skipped generation; no suitable constructor could be found. Data: {}", node);
                            return;
                        }

                        getReference(id, true).set(Polyfill.uncheckedCast(generated));
                    }
                });

        return data.size();
    }
}
