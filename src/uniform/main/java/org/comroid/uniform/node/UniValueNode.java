package org.comroid.uniform.node;

import org.comroid.api.ValuePointer;
import org.comroid.api.ValueType;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.impl.StandardValueType;

import java.util.stream.Stream;

public interface UniValueNode extends Reference<Object>, UniNode, ValuePointer<Object> {
    UniValueNode NULL = UniValueNode.create(null, StandardValueType.VOID, null);

    @Override
    default NodeType getNodeType() {
        return NodeType.VALUE;
    }

    @Override
    default Stream<UniValueNode> stream() {
        return streamNodes();
    }

    @Override
    default Stream<UniValueNode> streamNodes() {
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
    @SuppressWarnings("TypeParameterExplicitlyExtendsObject")
    ValueType<? extends Object> getHeldType();

    static <T> UniValueNode create(SerializationAdapter seriLib, ValueType<T> type, T value) {
        if (value == null)
            return NULL;
        return null;
    }
}
