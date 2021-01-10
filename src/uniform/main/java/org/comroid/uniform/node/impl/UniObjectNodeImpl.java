package org.comroid.uniform.node.impl;

import org.comroid.api.HeldType;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UniObjectNodeImpl
        extends AbstractUniNode<String, KeyedReference<String, UniNode>, Map<String, Object>>
        implements UniObjectNode {
    public <BAS, OBJ extends BAS> UniObjectNodeImpl(SerializationAdapter<BAS, OBJ, ?> seriLib, Map<String, Object> baseNode) {
        super(seriLib, baseNode);
    }

    @Override
    public boolean containsKey(Object key) {
        return accessors.stream().map(Entry::getKey).anyMatch(key::equals);
    }

    @Override
    public boolean containsValue(Object value) {
        return accessors.stream().map(KeyedReference::getValue).anyMatch(value::equals);
    }

    @Override
    public Object get(Object key) {
        return wrapKey(key).ifPresentMap(accessors::get).getValue();
    }

    @Nullable
    @Override
    public Object put(String key, Object value) {
        return accessors.compute(key, r -> {
            if (r == null)
                //noinspection unchecked
                return (KeyedReference<String, UniNode>) generateAccessor(key);
            return r;
        }).setValue((UniNode) value);
    }

    @Override
    public Object remove(Object key) {
        KeyedReference<? super String, KeyedReference<String, UniNode>> ref
                = wrapKey(key).ifPresentMap(accessors::getReference);
        UniNode node = null;
        if (ref != null) {
            node = ref.into(Rewrapper::get);
            ref.unset();
        }
        return node;
    }

    @Override
    public @NotNull <T> UniNode put(final String key, final HeldType<T> type, final T value)
            throws UnsupportedOperationException {
        return accessors.compute(key, ref -> {
            if (value instanceof UniObjectNode || value instanceof UniArrayNode) {
                return KeyedReference.create(key, (UniNode) value);
            } else {
                //noinspection unchecked
                final UniValueNodeImpl valueNode = new UniValueNodeImpl(key, seriLib, seriLib.createValueAdapter(value));
                if (ref != null) {
                    ref.set(valueNode);
                    return ref;
                }
                return KeyedReference.create(key, valueNode);
            }
        }).getValue();
    }

    @Override
    public void putAll(@NotNull Map<? extends String, ?> map) {
        map.forEach(this::put);
    }

    @NotNull
    @Override
    public Set<String> keySet() {
        return Collections.unmodifiableSet(accessors.stream()
                .map(Entry::getKey)
                .collect(Collectors.toSet()));
    }

    @NotNull
    @Override
    public Collection<Object> values() {
        return Collections.unmodifiableList(accessors.stream()
                .map(Entry::getValue)
                .collect(Collectors.toList()));
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return Collections.unmodifiableSet(accessors.stream()
                .map(Polyfill::<Map.Entry<String, Object>>uncheckedCast)
                .collect(Collectors.toSet()));
    }

    @Override
    protected KeyedReference<String, ? extends UniNode> generateAccessor(final String key) {
        return new KeyedReference.Support.Base<String, UniNode>(false, key, null) {
            @Override
            public boolean isOutdated() {
                return true;
            }

            @Override
            protected UniNode doGet() {
                final Object value = baseNode.get(key);
                assert getNodeType() == NodeType.OBJECT;
                //noinspection unchecked
                return new UniValueNodeImpl(key, seriLib, seriLib.createValueAdapter(value));
            }

            @Override
            protected boolean doSet(UniNode value) {
                switch (value.getNodeType()) {
                    case OBJECT:
                        Map<String, Object> map = new HashMap<>(value.asObjectNode());
                        return baseNode.put(key, map) != value;
                    case ARRAY:
                        ArrayList<UniNode> list = new ArrayList<>(value.asArrayNode());
                        return baseNode.put(key, list) != value;
                }

                return baseNode.put(key, value.asRaw(null)) != value;
            }
        };
    }

    @Override
    protected Stream<String> streamKeys() {
        return Stream.concat(
                baseNode.keySet().stream(),
                accessors.stream().map(Map.Entry::getKey
                )).distinct();
    }
}
