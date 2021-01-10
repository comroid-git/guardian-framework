package org.comroid.uniform.node.impl;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import sun.jvm.hotspot.gc.shared.CardGeneration;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class AbstractUniNode<AcK, Ref extends Reference<? extends UniNode>, Bas> implements UniNode {
    private final UniNode parent;
    protected final Bas baseNode;
    protected final SerializationAdapter<Object, Object, Object> seriLib;
    protected final ReferenceMap<AcK, UniNode> accessors = new ReferenceMap.Support.Basic<>(
            new ConcurrentHashMap<>(), ack -> Polyfill.uncheckedCast(wrapKey(ack).ifPresentMap(this::generateAccessor)));

    @Override
    public SerializationAdapter<?, ?, ?> getSerializationAdapter() {
        return seriLib;
    }

    @Override
    public String getName() {
        return "<root node>";
    }

    protected AbstractUniNode(SerializationAdapter seriLib, @Nullable UniNode parent, Bas baseNode) {
        this.seriLib = seriLib;
        this.parent = parent;
        this.baseNode = baseNode;
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
        return Polyfill.uncheckedCast(Objects.requireNonNull(accessors.get(key), MessageSupplier.format("Missing accessor for key %s", key)));
    }

    @Override
    public boolean has(String fieldName) {
        return UniNode.unsupported(this, "HAS_KEY", NodeType.OBJECT);
    }

    @Override
    public UniNode copyFrom(@NotNull UniNode it) {
        return null; // todo
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

    @Override
    public String getAlternateFormattedName() {
        return String.format("UniNode<%s=%s>", getNodeType(), toString());
    }
}
