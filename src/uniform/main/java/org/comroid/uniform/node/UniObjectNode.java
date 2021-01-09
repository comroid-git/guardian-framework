package org.comroid.uniform.node;

import java.util.Map;

public interface UniObjectNode extends Map<String, Object>, UniNode {
    @Override
    default Type getNodeType() {
        return Type.OBJECT;
    }
}
