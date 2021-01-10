package org.comroid.uniform.node;

import org.comroid.api.ValuePointer;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.impl.StandardValueType;

import java.util.stream.Stream;

public interface UniValueNode extends Reference, UniNode, ValuePointer {
    UniValueNode NULL = UniValueNode.create(null, StandardValueType.VOID, null);

    @Override
    default NodeType getNodeType() {
        return NodeType.VALUE;
    }

    @Override
    default Stream<? extends UniNode> stream() {
        return streamNodes();
    }

    @Override
    default Stream<? extends UniNode> streamNodes() {
        return Stream.of(this);
    }

    @Override
    default boolean isNull() {
        return getNodeType() == null;
    }

    @Override
    default boolean isNonNull() {
        return !isNull();
    }

    @Override
    ValueType getHeldType();

    static <T> UniValueNode create(SerializationAdapter seriLib, ValueType<T> type, T value) {
        if (value == null)
            return NULL;
        return null;
    }
}
