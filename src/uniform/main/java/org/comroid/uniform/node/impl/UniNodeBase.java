package org.comroid.uniform.node.impl;

import org.comroid.api.*;
import org.comroid.mutatio.ref.Processor;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

public abstract class UniNodeBase implements UniNode {
    protected final SerializationAdapter<?, ?, ?> serializationAdapter;
    private final Type type;
    private final Map<String, Reference<String>> baseAccessors = new ConcurrentHashMap<>();
    private final Map<String, Processor<UniNode>> wrappedAccessors = new ConcurrentHashMap<>();

    @Override
    public String getSerializedString() {
        return toString();
    }

    public abstract Object getBaseNode();

    @Override
    public final String getMimeType() {
        return serializationAdapter.getMimeType();
    }

    protected UniNodeBase(SerializationAdapter<?, ?, ?> serializationAdapter, Type type) {
        this.serializationAdapter = serializationAdapter;
        this.type = type;
    }

    protected String unwrapDST(Object o) {
        if (o instanceof UniNodeBase)
            return o.toString();
        return String.valueOf(o);
    }

    @Override
    public final String toString() {
        return getBaseNode().toString();
    }

    protected Processor<UniNode> computeNode(
            String fieldName,
            Supplier<Reference<String>> referenceSupplier
    ) {
        final Reference<String> base = baseAccessors.computeIfAbsent(fieldName, key -> referenceSupplier.get());
        return wrappedAccessors.computeIfAbsent(fieldName, key -> base.process()
                .map(str -> {
                    try {
                        DataStructureType<? extends SerializationAdapter<?, ?, ?>, ?, ?> dst = serializationAdapter.typeOfData(str);

                        if (dst != null) switch (dst.typ) {
                            case OBJECT:
                                return serializationAdapter.parse(str).asObjectNode();
                            case ARRAY:
                                return serializationAdapter.parse(str).asArrayNode();
                        }
                    } catch (IllegalArgumentException ignored) {
                    }

                    return new UniValueNode<>(serializationAdapter, base, ValueTypeBase.STRING);
                }));
    }

    protected void set(Object value) {
        unsupported(this, "SET", Type.VALUE);
    }

    @Nullable
    protected <T> UniNode unwrapNode(String key, HeldType<T> type, T value) {
        if (value instanceof UniNodeBase)
            return put(key, ValueTypeBase.VOID, Polyfill.uncheckedCast(((UniNodeBase) value).getBaseNode()));
        if (Stream.of(serializationAdapter.objectType, serializationAdapter.arrayType)
                .anyMatch(dst -> dst.typeClass().isInstance(value)) && type != ValueTypeBase.VOID)
            return put(key, ValueTypeBase.VOID, Polyfill.uncheckedCast(value));
        return null;
    }

    public interface Adapter<B> {
        B getBaseNode();
    }
}
