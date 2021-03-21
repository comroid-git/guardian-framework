package org.comroid.uniform.node;

import org.comroid.annotations.inheritance.MustExtend;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.impl.UniObjectNodeImpl;

import java.util.Map;

@MustExtend(UniObjectNodeImpl.class)
public interface UniObjectNode extends Map<String, Object>, UniNode {
    @Override
    default NodeType getNodeType() {
        return NodeType.OBJECT;
    }
}
