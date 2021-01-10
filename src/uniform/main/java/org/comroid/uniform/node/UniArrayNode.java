package org.comroid.uniform.node;

import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.model.NodeType;

import java.util.List;
import java.util.stream.Stream;

public interface UniArrayNode extends List<UniNode>, UniNode {
    @Override
    default NodeType getNodeType() {
        return NodeType.ARRAY;
    }

    @Override
    default Stream<UniNode> stream() {
        return streamRefs().flatMap(Reference::stream);
    }
}
