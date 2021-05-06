package org.comroid.uniform.model;

import org.comroid.api.Named;
import org.jetbrains.annotations.Nullable;

public enum NodeType implements Named {
    OBJECT(DataStructureType.Primitive.OBJECT),
    ARRAY(DataStructureType.Primitive.ARRAY),
    VALUE(null);

    public final @Nullable DataStructureType.Primitive dst;

    @Override
    public String getName() {
        return name();
    }

    NodeType(@Nullable DataStructureType.Primitive primitive) {
        this.dst = primitive;
    }
}
