package org.comroid.uniform.node.impl;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class UniObjectNodeImpl extends AbstractUniNode<String, KeyedReference<String, UniNode>> implements UniObjectNode {
    public <BAS, OBJ extends BAS> UniObjectNodeImpl(SerializationAdapter<BAS, OBJ, ?> seriLib, OBJ baseNode) {
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
                return generateAccessor(key);
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
    protected KeyedReference<String, UniNode> generateAccessor(String ack) {
        return null;
    }
}
