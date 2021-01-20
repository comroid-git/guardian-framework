package org.comroid.uniform.node.impl;

import org.comroid.api.*;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UniObjectNodeImpl
        extends AbstractUniNode<String, KeyedReference<String, UniNode>, Map<String, Object>>
        implements UniObjectNode {
    public <BAS, OBJ extends BAS> UniObjectNodeImpl(SerializationAdapter<BAS, OBJ, ?> seriLib, @Nullable UniNode parent, Map<String, Object> baseNode) {
        super(seriLib, parent, baseNode);
    }

    @Override
    public boolean has(String fieldName) {
        return containsKey(fieldName);
    }

    @Override
    public boolean containsKey(Object key) {
        return streamRefs().anyMatch(ref -> ref.getKey().equals(key));
    }

    @Override
    public boolean containsValue(Object value) {
        return streamNodes().filter(UniValueNode.class::isInstance)
                .anyMatch(node -> node.asRaw().equals(value));
    }

    @Override
    public int size() {
        return baseNode.size();
    }

    @Override
    public Object get(Object key) {
        return wrapKey(key).ifPresentMap(accessors::get);
    }

    @Nullable
    @Override
    public Object put(String key, Object value) {
        if (value instanceof UniNode) {
            ValueType<UniNode> nodetype = Polyfill.uncheckedCast(
                    ((UniNode) value).getNodeType() == NodeType.OBJECT
                            ? seriLib.getObjectType()
                            : seriLib.getArrayType());
            return put(key, nodetype, (UniNode) value);
        } else return put(key, StandardValueType.typeOf(value), value);
    }

    @Override
    public Object remove(Object key) {
        KeyedReference<String, UniNode> ref = wrapKey(key).ifPresentMap(accessors::getReference);
        UniNode node = null;
        if (ref != null) {
            node = ref.get();
            ref.unset();
        }
        return node;
    }

    @Override
    public @NotNull <T> UniNode put(final String key, final ValueType<? extends T> type, final T value)
            throws UnsupportedOperationException {
        //noinspection ConstantConditions
        return accessors.compute(key, node -> {
            if (value == null)
                return null;
            else if (value instanceof UniObjectNode || value instanceof UniArrayNode)
                return (UniNode) value;
            else {
                UniValueNodeImpl valueNode = new UniValueNodeImpl(key, seriLib, this, seriLib
                        .createValueAdapter(value, nv -> baseNode.put(key, nv) != nv));
                return valueNode;
            }
        });
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ?> map) {
        map.forEach(this::put);
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(streamRefs()
                .map(Entry::getKey)
                .collect(Collectors.toSet()));
    }

    @NotNull
    @Override
    public Collection<Object> values() {
        return Collections.unmodifiableList(streamRefs()
                .map(Entry::getValue)
                .map(UniNode::asRaw)
                .collect(Collectors.toList()));
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return Collections.unmodifiableSet(streamRefs()
                .map(ref -> new AbstractMap.SimpleImmutableEntry<>(ref.getKey(), ref.into(uniNode -> uniNode.asRaw())))
                .collect(Collectors.toSet()));
    }

    @Override
    protected KeyedReference<String, UniNode> generateAccessor(final String key) {
        return new KeyedReference.Support.Base<String, UniNode>(true, key, null) {
/*
            @Override
            public boolean isOutdated() {
                return true;
            }
*/
            @Override
            protected UniNode doGet() {
                final Object value = baseNode.get(key);

                assert getNodeType() == NodeType.OBJECT;

                if (value == null)
                    return UniValueNode.NULL;
                if (seriLib.getObjectType().test(value)) {
                    // value is object
                    return seriLib.createUniObjectNode(value);
                } else if (seriLib.getArrayType().test(value)) {
                    // value is array
                    return seriLib.createUniArrayNode(value);
                } else return new UniValueNodeImpl(key, seriLib, UniObjectNodeImpl.this, seriLib
                        .createValueAdapter(value, nv -> baseNode.put(key, nv) != nv));
            }

            @Override
            protected boolean doSet(UniNode value) {
                if (value == null) {
                    baseNode.put(key, null);
                    return true;
                }
                if (value instanceof UniValueNode)
                    return baseNode.put(key, value.asRaw()) != value;
                return baseNode.put(key, value.getBaseNode()) != value;
            }
        };
    }

    @Override
    protected Stream<String> streamKeys() {
        return Stream.concat(
                baseNode.keySet().stream().filter(key -> baseNode.get(key) != null),
                accessors.stream(any -> true).map(Map.Entry::getKey)
        ).distinct();
    }
}
