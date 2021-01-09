package org.comroid.uniform.node;

import java.util.List;

public interface UniArrayNode extends List<UniNode>, UniNode {
    @Override
    default Type getNodeType() {
        return Type.ARRAY;
    }
}
