package org.comroid.uniform.model;

import org.comroid.api.StringSerializable;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;

public interface Serializable extends StringSerializable {
    UniNode toUniNode();

    default UniObjectNode toObjectNode() {
        return toUniNode().asObjectNode();
    }

    default UniArrayNode toArrayNode() {
        return toUniNode().asArrayNode();
    }

    @Override
    default String toSerializedString() {
        return toUniNode().toString();
    }
}
