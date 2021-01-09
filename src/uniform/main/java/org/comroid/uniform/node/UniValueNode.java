package org.comroid.uniform.node;

import org.comroid.api.ValuePointer;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.impl.StandardValueType;

public interface UniValueNode extends Reference, UniNode, ValuePointer {
    UniValueNode NULL = UniValueNode.create(null, StandardValueType.VOID, null);

    @Override
    default Type getNodeType() {
        return Type.VALUE;
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
