package org.comroid.varbind;

import org.comroid.api.ContextualProvider;
import org.comroid.common.io.FileHandle;
import org.comroid.common.io.FileProcessor;
import org.comroid.uniform.Context;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.NotNull;

import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class FileConfiguration extends DataContainerBase<FileConfiguration> implements FileProcessor {
    private final Context context;
    private final FileHandle file;
    private final CharSequence mimeType;
    private final UUID uuid = UUID.randomUUID();

    @Override
    public final FileHandle getFile() {
        return file;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    public FileConfiguration(
            Context context,
            FileHandle file,
            CharSequence mimeType
    ) {
        super(context);

        this.context = context;
        this.file = file;
        this.mimeType = mimeType;

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
        final UniNode data = context.parse(mimeType, file.getContent());

        if (data != null)
            return updateFrom(data.asObjectNode()).size();
        return 0;
    }
}
