package org.comroid.uniform.node;

import org.comroid.annotations.inheritance.MustExtend;
import org.comroid.api.ValueType;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.model.Serializable;
import org.comroid.uniform.node.impl.UniArrayNodeImpl;
import org.comroid.util.StandardValueType;

import java.util.List;
import java.util.stream.Stream;

@MustExtend(UniArrayNodeImpl.class)
public interface UniArrayNode extends List<UniNode>, UniNode {
    @Override
    default NodeType getNodeType() {
        return NodeType.ARRAY;
    }

    @Override
    default Stream<UniNode> stream() {
        return streamRefs().flatMap(Reference::stream);
    }

    default boolean addValue(Object value) {
        if (value instanceof UniNode)
            return add((UniNode) value);
        if (value instanceof Serializable)
            return add(((Serializable) value).toUniNode());
        ValueType<Object> tof = StandardValueType.typeOf(value);
        add(tof, value);
        return true;
    }
}
