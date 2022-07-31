package org.comroid.uniform.node;

import org.comroid.annotations.inheritance.MustExtend;
import org.comroid.api.Rewrapper;
import org.comroid.api.ValuePointer;
import org.comroid.api.ValueType;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.impl.UniValueNodeImpl;
import org.comroid.util.StandardValueType;

import java.util.stream.Stream;

@MustExtend(UniValueNodeImpl.class)
public interface UniValueNode extends Rewrapper<Object>, UniNode, ValuePointer<Object> {
    @Override
    default NodeType getNodeType() {
        return NodeType.VALUE;
    }

    UniValueNode NULL = UniValueNode.create(null, StandardValueType.VOID, null);

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

    @Override
    default Stream<UniValueNode> stream() {
        return streamNodes();
    }

    @Override
    default Stream<UniValueNode> streamNodes() {
        return Stream.of(this);
    }


}
