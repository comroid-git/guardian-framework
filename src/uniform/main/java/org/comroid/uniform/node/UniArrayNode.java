package org.comroid.uniform.node;

import java.util.List;
import java.util.stream.Stream;

public interface UniArrayNode extends List<UniNode>, UniNode {
    @Override
    default Type getNodeType() {
        return Type.ARRAY;
    }

    @Override
    default Stream<UniNode> stream() {
        return streamNodes().map(UniNode.class::cast);
    }
}
