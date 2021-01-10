package org.comroid.uniform.node.impl;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.CachedValue;
import org.comroid.mutatio.cache.ValueUpdateListener;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class UniValueNodeImpl extends AbstractUniNode<Void, Reference<UniNode>, Reference<Object>> implements UniValueNode {
    private final String name;
    private final ValueAdapter<?, Object> valueAdapter;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isOutdated() {
        return baseNode.isOutdated();
    }

    @Override
    public Collection<? extends CachedValue<?>> getDependents() {
        return baseNode.getDependents();
    }

    @Override
    public Rewrapper<? extends Reference<?>> getParent() {
        return baseNode.getParent();
    }

    @Override
    public boolean isMutable() {
        return baseNode.isMutable();
    }

    @Override
    public ValueType getHeldType() {
        return valueAdapter.getValueType();
    }

    public UniValueNodeImpl(String name, SerializationAdapter seriLib, ValueAdapter<? extends Object, Object> valueAdapter) {
        super(seriLib, Reference.provided(valueAdapter::asActualType));

        this.name = name;
        this.valueAdapter = valueAdapter;
    }

    @Override
    public void rebind(Supplier behind) {
        baseNode.rebind(behind);
    }

    @Override
    public void cleanupDependents() {
        baseNode.cleanupDependents();
    }

    @Override
    public Object update(Object withValue) {
        return baseNode.update(withValue);
    }

    @Override
    public boolean outdate() {
        return baseNode.outdate();
    }

    @Override
    public boolean attach(ValueUpdateListener listener) {
        return baseNode.attach(listener);
    }

    @Override
    public boolean detach(ValueUpdateListener listener) {
        return baseNode.detach(listener);
    }

    @Override
    public boolean addDependent(CachedValue dependency) {
        return baseNode.addDependent(dependency);
    }

    @Nullable
    @Override
    public Object get() {
        return valueAdapter.asActualType();
    }

    @Override
    protected KeyedReference<Void, ? extends UniNode> generateAccessor(Void nil) {
        return new KeyedReference.Support.Base<Void, UniNode>(false, nil, null) {
            @Override
            public boolean isOutdated() {
                return true;
            }

            @Override
            protected UniNode doGet() {
                final Object value = valueAdapter.asActualType();
                assert getNodeType() == NodeType.VALUE;
                return new UniValueNodeImpl(name, seriLib, valueAdapter);
            }

            @Override
            protected boolean doSet(UniNode value) {
                switch (value.getNodeType()) {
                    case OBJECT:
                        Map<String, Object> map = new HashMap<>(value.asObjectNode());
                        return baseNode.set(map);
                    case ARRAY:
                        ArrayList<UniNode> list = new ArrayList<>(value.asArrayNode());
                        return baseNode.set(list);
                }

                return baseNode.set(value.asRaw(null));
            }
        };
    }
}
