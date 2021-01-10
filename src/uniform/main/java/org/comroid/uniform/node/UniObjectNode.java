package org.comroid.uniform.node;

import org.comroid.uniform.model.NodeType;

import java.util.Map;

public interface UniObjectNode extends Map<String, Object>, UniNode {
    @Override
    default NodeType getNodeType() {
        return NodeType.OBJECT;
    }
}
