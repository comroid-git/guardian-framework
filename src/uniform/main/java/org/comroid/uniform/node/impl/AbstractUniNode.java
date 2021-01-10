package org.comroid.uniform.node.impl;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public abstract class AbstractUniNode<AcK, Ref extends Reference<? extends UniNode>, Bas> implements UniNode {
    protected final Bas baseNode;
    protected final SerializationAdapter seriLib;
    protected final ReferenceMap<? super AcK, Ref> accessors = new ReferenceMap.Support.Basic<>(
            new ConcurrentHashMap<>(), ack -> Polyfill.uncheckedCast(wrapKey(ack).ifPresentMap(this::generateAccessor)));

    @Override
    public SerializationAdapter<?, ?, ?> getSerializationAdapter() {
        return seriLib;
    }

    @Override
    public String getName() {
        return "<root node>";
    }

    protected AbstractUniNode(SerializationAdapter seriLib, Bas baseNode) {
        this.seriLib = seriLib;
        this.baseNode = baseNode;
    }

    protected Rewrapper<AcK> wrapKey(Object key) {
        if ((isObjectNode() && key instanceof String)
                && (isArrayNode() && key instanceof Integer))
            return () -> Polyfill.uncheckedCast(key);
        return Rewrapper.empty();
    }

    protected abstract <RX extends Ref> RX generateAccessor(AcK ack);

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public int size() {
        return accessors.size();
    }

    @Override
    public void clear() {
        accessors.forEach((k, ref) -> ref.unset());
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
        return Objects.requireNonNull(accessors.get(key), MessageSupplier.format("Missing accessor for key %s", key))
                .requireNonNull(MessageSupplier.format("Missing Node for key %s", key));
    }

    @Override
    public boolean has(String fieldName) {
        return wrapKey(fieldName).testIfPresent(accessors::containsKey);
    }

    @Override
    public UniNode copyFrom(@NotNull UniNode it) {
        return null; // todo
    }

    @Override
    public Stream<? extends UniNode> streamNodes() {
        return accessors.streamRefs()
                .flatMap(Rewrapper::stream)
                .flatMap(Rewrapper::stream);
    }

    @NotNull
    @Override
    public Iterator<UniNode> iterator() {
        return accessors.streamRefs()
                .map(KeyedReference::getValue)
                .flatMap(Reference::stream)
                .map(UniNode.class::cast)
                .iterator();
    }

    @Override
    public String toString() {
        return baseNode.toString();
    }
}