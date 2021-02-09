package org.comroid.uniform.node.impl;

import org.comroid.api.Rewrapper;
import org.comroid.api.ValueType;
import org.comroid.mutatio.cache.SingleValueCache;
import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.cache.ValueUpdateListener;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.comroid.util.StandardValueType.*;

public final class UniValueNodeImpl extends AbstractUniNode<Void, Reference<UniNode>, Reference<Object>> implements UniValueNode, SingleValueCache<Object> {
    private final String name;
    private final ValueAdapter<?, Object> valueAdapter;

    @Override
    public String getName() {
        return name;
    }

    @Override
    protected Stream<Void> streamKeys() {
        return Stream.empty();
    }

    @Override
    public boolean isOutdated() {
        return baseNode.isOutdated();
    }

    @Override
    public boolean isUpToDate() {
        return baseNode.isUpToDate();
    }

    @Override
    public void updateCache() {
        baseNode.updateCache();
    }

    @Override
    public Collection<? extends ValueCache<?>> getDependents() {
        return baseNode.getDependents();
    }

    @Override
    public int deployListeners(Object forValue, Executor executor) {
        return baseNode.deployListeners(forValue, executor);
    }

    @Override
    public Rewrapper<? extends Reference<?>> getParent() {
        return baseNode.getParent();
    }

    @Override
    public ValueType<Object> getHeldType() {
        return valueAdapter.getValueType();
    }

    public UniValueNodeImpl(String name, SerializationAdapter seriLib, @Nullable UniNode parent, ValueAdapter<? extends Object, Object> valueAdapter) {
        super(seriLib, parent, Reference.provided(valueAdapter::asActualType));

        this.name = name;
        this.valueAdapter = valueAdapter;
    }

    @Override
    public Object putIntoCache(Object withValue) {
        return baseNode.putIntoCache(withValue);
    }

    @Override
    public Object getFromCache() {
        return baseNode.getFromCache();
    }

    @Override
    public void outdateCache() {
        baseNode.outdateCache();
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
    public boolean addDependent(ValueCache dependency) {
        return baseNode.addDependent(dependency);
    }

    @Nullable
    @Override
    public Object get() {
        return valueAdapter.asActualType();
    }

    @Override
    protected KeyedReference<Void, UniNode> generateAccessor(Void nil) {
        return new KeyedReference.Support.Base<Void, UniNode>(nil, null, true) {
            /*
                        @Override
                        public boolean isOutdated() {
                            return true;
                        }
            */
            @Override
            protected UniNode doGet() {
                final Object value = valueAdapter.asActualType();
                if (value == null)
                    return UniValueNode.NULL;
                assert getNodeType() == NodeType.VALUE;
                return new UniValueNodeImpl(name, seriLib, null, valueAdapter);
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

    @Override
    public <R> R as(ValueType<R> type) {
        return returnAs(type, null);
    }

    @Override
    public Object asRaw() {
        return valueAdapter.asActualType();
    }

    @Override
    public Object asRaw(@Nullable Object fallback) {
        return asRaw();
    }

    @Override
    public String asString() {
        return asString(null);
    }

    @Override
    public String asString(@Nullable String fallback) {
        return returnAs(STRING, fallback);
    }

    @Override
    public boolean asBoolean() {
        return returnAs(BOOLEAN, null);
    }

    @Override
    public boolean asBoolean(boolean fallback) {
        return returnAs(BOOLEAN, fallback);
    }

    @Override
    public int asInt() {
        return returnAs(INTEGER, null);
    }

    @Override
    public int asInt(int fallback) {
        return returnAs(INTEGER, fallback);
    }

    @Override
    public long asLong() {
        return returnAs(LONG, null);
    }

    @Override
    public long asLong(long fallback) {
        return returnAs(LONG, fallback);
    }

    @Override
    public double asDouble() {
        return returnAs(DOUBLE, null);
    }

    @Override
    public double asDouble(double fallback) {
        return returnAs(DOUBLE, fallback);
    }

    @Override
    public float asFloat() {
        return returnAs(FLOAT, null);
    }

    @Override
    public float asFloat(float fallback) {
        return returnAs(FLOAT, fallback);
    }

    @Override
    public short asShort() {
        return returnAs(SHORT, null);
    }

    @Override
    public short asShort(short fallback) {
        return returnAs(SHORT, fallback);
    }

    @Override
    public char asChar() {
        return returnAs(CHARACTER, null);
    }

    @Override
    public char asChar(char fallback) {
        return returnAs(CHARACTER, fallback);
    }

    @Contract("null, _ -> fail; _, null -> _")
    private <R> R returnAs(ValueType<R> type, R fallback) {
        if (type == null)
            throw new IllegalArgumentException("Missing Type");
        if (valueAdapter.getValueType().equals(type))
            //noinspection unchecked
            return (R) valueAdapter.asActualType();
        try {
            R yield = valueAdapter.asType(type);
            return yield == null ? fallback : yield;
        } catch (IllegalArgumentException iaex) {
            if (fallback == null)
                throw iaex;
            return fallback;
        }
    }
}
