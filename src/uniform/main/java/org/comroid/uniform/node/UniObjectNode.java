package org.comroid.uniform.node;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface UniObjectNode extends Map<String, Object>, UniNode {
    @Override
    default Type getNodeType() {
        return Type.OBJECT;
    }
}
