package org.comroid.uniform.node.impl;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.api.ValueType;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Stream;

public abstract class AbstractUniNode<AcK, Ref extends KeyedReference<AcK, UniNode>, Bas> implements UniNode {
    protected final Bas baseNode;
    protected final SerializationAdapter<Object, Object, Object> seriLib;
    protected final ReferenceMap<AcK, UniNode> accessors = new ReferenceMap<AcK, UniNode>() {
        {
            setMutable(true);
        }

        @Override
        protected KeyedReference<AcK, UniNode> createEmptyRef(AcK key) {
            return generateAccessor(key);
        }
    };
    private final UniNode parent;

    @Override
    public SerializationAdapter<?, ?, ?> getSerializationAdapter() {
        return seriLib;
    }

    @Override
    public String getName() {
        return "<root node>";
    }

    @Override
    public Object getBaseNode() {
        return baseNode;
    }

    @Override
    public Rewrapper<? extends UniNode> getParentNode() {
        return parent == null ? Rewrapper.empty() : () -> parent;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public String getAlternateFormattedName() {
        return String.format("UniNode<%s=%s>", getNodeType(), toString());
    }

    protected AbstractUniNode(SerializationAdapter seriLib, @Nullable UniNode parent, Bas baseNode) {
        this.seriLib = seriLib;
        this.parent = parent;
        this.baseNode = baseNode;
    }

    @Override
    public final Rewrapper<Object> getData(final Object key) {
        if (isObjectNode() && has((String) key))
            return () -> get((String) key);
        if (isArrayNode() && has((int) key))
            return () -> get((int) key);
        if (isValueNode())
            return asValueNode();
        return Rewrapper.empty();
    }

    protected Rewrapper<AcK> wrapKey(Object key) {
        if ((isObjectNode() && key instanceof String)
                || (isArrayNode() && key instanceof Integer))
            return () -> Polyfill.uncheckedCast(key);
        return Rewrapper.empty();
    }

    protected abstract Ref generateAccessor(AcK ack);

    protected abstract Stream<AcK> streamKeys();

    @Override
    public Stream<Ref> streamRefs() {
        return streamKeys().map(this::generateAccessor);
    }

    @Override
    public int size() {
        return (int) streamKeys().count();
    }

    @Override
    public void clear() {
        accessors.streamRefs().forEach(Reference::unset);
        accessors.clear();
    }

    @Override
    public @NotNull UniNode get(int index) {
        return wrapKey(index).ifPresentMapOrElseGet(this::getAccessor, () -> UniValueNode.NULL);
    }

    @Override
    public @NotNull UniNode get(String fieldName) {
        return wrapKey(fieldName).ifPresentMapOrElseGet(this::getAccessor, () -> UniValueNode.NULL);
    }

    private UniNode getAccessor(AcK key) {
        return Polyfill.uncheckedCast(Objects.requireNonNull(accessors.getReference(key, true).getValue(),
                MessageSupplier.format("Missing accessor for key %s; data = %s", key, toSerializedString())));
    }

    @Override
    public boolean has(String fieldName) {
        return UniNode.unsupported(this, "HAS_KEY", NodeType.OBJECT);
    }

    @Override
    @SuppressWarnings("unchecked")
    public UniNode surroundWith(NodeType type, Object fname) {
        switch (type) {
            case OBJECT:
                AbstractUniNode<String, KeyedReference<String, UniNode>, Map<String, Object>> newObj = (AbstractUniNode<String, KeyedReference<String, UniNode>, Map<String, Object>>) seriLib.createObjectNode();
                newObj.accessors.put((String) fname, this);
                return newObj;
            case ARRAY:
                AbstractUniNode<Integer, KeyedReference<Integer, UniNode>, List<Object>> newArr = (AbstractUniNode<Integer, KeyedReference<Integer, UniNode>, List<Object>>) seriLib.createArrayNode();
                newArr.accessors.put(0, this);
                return newArr;
            case VALUE:
                throw new UnsupportedOperationException("Cannot surround with ValueNode");
        }
        throw new AssertionError("unreachable");
    }

    @Override
    public final UniNode copyFrom(@NotNull UniNode it) {
        NodeType myType = getNodeType();
        NodeType otherType = it.getNodeType();
        if (myType != otherType)
            throw new IllegalArgumentException(String.format("Cannot copy from %s into %s", otherType, myType));
        if (!(it instanceof AbstractUniNode))
            throw new IllegalArgumentException("Data has Illegal type; does not extend AbstractUniNode");
        //noinspection unchecked
        ((AbstractUniNode) it).streamKeys()
                .forEach(k -> {
                    wrapKey(k).ifPresentOrElseThrow(
                            key -> it.getData(key).ifPresent(data -> this.set(key, data)),
                            () -> new AssertionError("Could not wrap key: " + k)
                    );
                });
        return this;
    }

    private void set(AcK key, Object data) {
        ValueType<Object> typeOf = StandardValueType.typeOf(data);
        if (key instanceof String)
            put((String) key, typeOf, data);
        else put((int) key, typeOf, data);
    }

    @NotNull
    @Override
    public Iterator<UniNode> iterator() {
        return streamNodes().map(UniNode.class::cast).iterator();
    }

    @Override
    public String toString() {
        return baseNode.toString();
    }
}
