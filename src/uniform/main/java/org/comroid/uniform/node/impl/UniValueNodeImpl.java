package org.comroid.uniform.node.impl;

import org.comroid.api.Rewrapper;
import org.comroid.mutatio.cache.CachedValue;
import org.comroid.mutatio.cache.ValueUpdateListener;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Supplier;

public final class UniValueNodeImpl extends AbstractUniNode<Void, Reference<UniNode>> implements UniValueNode {
    private final ValueType<Object> type;
    private final Reference<Object> value;

    @Override
    public boolean isOutdated() {
        return false;
    }

    @Override
    public Collection<? extends CachedValue<?>> getDependents() {
        return null;
    }

    @Override
    public Rewrapper<? extends Reference<?>> getParent() {
        return null;
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public ValueType getHeldType() {
        return null;
    }

    protected UniValueNodeImpl(SerializationAdapter seriLib, Object baseNode) {
        super(seriLib, baseNode);
    }

    @Override
    public void rebind(Supplier behind) {

    }

    @Override
    public void cleanupDependents() {

    }

    @Override
    public Object update(Object withValue) {
        return null;
    }

    @Override
    public boolean outdate() {
        return false;
    }

    @Override
    public boolean attach(ValueUpdateListener listener) {
        return false;
    }

    @Override
    public boolean detach(ValueUpdateListener listener) {
        return false;
    }

    @Override
    public boolean addDependent(CachedValue dependency) {
        return false;
    }

    @Nullable
    @Override
    public Object get() {
        return null;
    }

    @Override
    protected Reference<UniNode> generateAccessor(Void ack) {
        return null;
    }
}
