package org.comroid.uniform.model;

import org.comroid.api.StringSerializable;
import org.comroid.uniform.node.UniNode;

public interface Serializable extends StringSerializable {
    UniNode toUniNode();

    @Override
    default String toSerializedString() {
        return toUniNode().toString();
    }
}
