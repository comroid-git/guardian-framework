package org.comroid.varbind;

import com.google.common.flogger.FluentLogger;
import org.comroid.api.Junction;
import org.comroid.api.Polyfill;
import org.comroid.common.Disposable;
import org.comroid.common.io.FileHandle;
import org.comroid.common.io.FileProcessor;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class FileCache<K, V extends DataContainer<V>, D>
        extends DataContainerCache<K, V, D>
        implements FileProcessor, Disposable {
    public static final FluentLogger logger = FluentLogger.forEnclosingClass();
    private final SerializationAdapter<?, ?, ?> seriLib;
    private final FileHandle file;
    private final UUID uuid = UUID.randomUUID();

    @Override
    public FileHandle getFile() {
        return file;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    public FileCache(
            SerializationAdapter<?, ?, ?> seriLib,
            VarBind<? super V, ?, ?, K> idBind,
            FileHandle file,
            int largeThreshold,
            D dependencyObject
    ) {
        this(seriLib, idBind, null, file, largeThreshold, false, dependencyObject);
    }

    public FileCache(
            SerializationAdapter<?, ?, ?> seriLib,
            VarBind<? super V, ?, ?, K> idBind,
            Junction<K, String> converter,
            FileHandle file,
            int largeThreshold,
            boolean keyCaching,
            D dependencyObject
    ) {
        super(largeThreshold, new ConcurrentHashMap<>(), idBind);

        this.seriLib = seriLib;
        this.file = file;

        reloadData();
    }

    @Override
    public synchronized int storeData() throws IOException {
        final UniArrayNode data = seriLib.createUniArrayNode(null);

        entryIndex()
                .stream()
                .map(Polyfill::<KeyedReference<K, V>>uncheckedCast)
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
        if (!uniNode.isArrayNode())
            throw new IllegalArgumentException("Data is not an array");

        data = uniNode.asArrayNode();
        data.asNodeList().stream()
                .filter(UniNode::isObjectNode)
                .map(UniNode::asObjectNode)
                .forEach(node -> {
                    final K id = idBind.getFrom(node);

                    if (containsKey(id)) {
                        getReference(id, false).requireNonNull().updateFrom(node);
                    } else {
                        final Object generated = tryConstruct(node).orElse(null);

                        if (generated == null) {
                            logger.at(Level.WARNING).log("Skipped generation; no suitable constructor could be found. Data: %s", node);
                            return;
                        }

                        getReference(id, true).set(Polyfill.uncheckedCast(generated));
                    }
                });

        return data.size();
    }
}
