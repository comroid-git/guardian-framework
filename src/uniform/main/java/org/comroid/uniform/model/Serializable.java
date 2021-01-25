package org.comroid.uniform.model;

import org.comroid.uniform.node.UniNode;

public interface Serializable {
    UniNode toUniNode();

    default String toSerializedString() {
        return toUniNode().toString();
    }
}
