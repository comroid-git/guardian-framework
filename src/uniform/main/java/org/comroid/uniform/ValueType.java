package org.comroid.uniform;

import org.comroid.api.HeldType;
import org.comroid.uniform.node.impl.ValueTypeBase;
import org.jetbrains.annotations.Nullable;

public interface ValueType<R> extends HeldType<R> {
    static <T> @Nullable ValueType<T> typeOf(T it) {
        return ValueTypeBase.typeOf(it);
    }
}
