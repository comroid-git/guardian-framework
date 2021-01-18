package org.comroid.varbind;

import org.comroid.api.ContextualProvider;
import org.comroid.common.io.FileHandle;
import org.comroid.common.io.FileProcessor;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class FileConfiguration extends DataContainerBase<FileConfiguration> implements FileProcessor {
    private final SerializationAdapter<?, ?, ?> serializationAdapter;
    private final FileHandle file;
    private final UUID uuid = UUID.randomUUID();

    @Override
    public final FileHandle getFile() {
        return file;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    public @NotNull SerializationAdapter<?, ?, ?> getFromContext() {
        return serializationAdapter;
    }

    public FileConfiguration(
            SerializationAdapter<?, ?, ?> serializationAdapter,
            FileHandle file
    ) {
        super(serializationAdapter, null);

        this.serializationAdapter = serializationAdapter;
        this.file = file;

        reloadData();
    }

    @Override
    public final int storeData() throws IOException {
        final UniObjectNode data = toObjectNode(this);

        try (FileWriter fw = new FileWriter(file, false)) {
            fw.append(data.toString());
        }

        return 1;
    }

    @Override
    public final int reloadData() {
        final UniNode data = serializationAdapter.createUniNode(file.getContent());

        if (data != null)
            return updateFrom(data.asObjectNode()).size();
        return 0;
    }
}
