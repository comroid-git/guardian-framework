package org.comroid.varbind.container;

import org.comroid.api.Builder;
import org.comroid.api.Invocable;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.ArrayUtil;
import org.comroid.varbind.bind.VarBind;

public final class DataContainerBuilder<T extends DataContainerBase<? super T>> implements Builder<T> {
    public final UniObjectNode data;
    private final Invocable<? extends T> invocable;
    private final Object[] otherArgs;

    public DataContainerBuilder(
            Class<? extends T> type,
            Object... otherArgs
    ) {
        this(type, UniObjectNode.dummy(), otherArgs);
    }

    public DataContainerBuilder(
            Class<? extends T> type,
            UniObjectNode data,
            Object... otherArgs
    ) {
        this.invocable = Invocable.ofClass(type);
        this.data = data;
        this.otherArgs = otherArgs;
    }

    @Override
    public T build() {
        return invocable.autoInvoke(ArrayUtil.insert(otherArgs, otherArgs.length, data));
    }

    public <V> DataContainerBuilder<T> setValue(VarBind<?, V, ?, ?> field, V value) {
        data.put(field, value);
        return this;
    }
}
