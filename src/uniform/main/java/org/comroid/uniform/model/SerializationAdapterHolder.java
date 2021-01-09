package org.comroid.uniform.model;

import org.comroid.api.ContextualProvider;
import org.comroid.uniform.SerializationAdapter;
import org.jetbrains.annotations.NotNull;

public interface SerializationAdapterHolder extends ContextualProvider.Member {
    @NotNull
    default SerializationAdapter<?, ?, ?> getFromContext() {
        return getSerializationAdapter();
    }

    SerializationAdapter<?, ?, ?> getSerializationAdapter();
}
