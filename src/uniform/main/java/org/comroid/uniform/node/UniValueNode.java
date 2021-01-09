package org.comroid.uniform.node;

import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.node.impl.ValueTypeBase;

public interface UniValueNode extends UniNode {
    UniValueNode<Void> NULL = new UniValueNode<>(null, Reference.empty(), ValueTypeBase.VOID);

    static <T> UniValueNode<T> empty() {
        //noinspection unchecked
        return (UniValueNode<T>) NULL;
    }

    @Deprecated
    static <T> UniValueNode<T> nullNode() {
        return empty();
    }
}
