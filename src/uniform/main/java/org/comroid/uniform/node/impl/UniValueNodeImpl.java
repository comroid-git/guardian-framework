package org.comroid.uniform.node.impl;

import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UniValueNodeImpl<T> extends UniNodeBase implements UniValueNode {
    private final Reference<T> baseReference;
    private final ValueType<T> targetType;

    @Override
    public Object getBaseNode() {
        return baseReference.get();
    }

    @Override
    public Type getType() {
        return null;
    }

    @Override
    public boolean isNull() {
        return baseReference.isNull();
    }

    @Override
    public SerializationAdapter<?, ?, ?> getSerializationAdapter() {
        return null;
    }

    get

    public UniValueNodeImpl(SerializationAdapter<?, ?, ?> seriLib, Reference<T> baseReference, ValueType<T> targetType) {
        super(seriLib, Type.VALUE);
        this.baseReference = baseReference;
        this.targetType = targetType;
    }

    private static String unwrapString(String str) {
        if (str.startsWith("\"") && str.endsWith("\""))
            return str.substring(1, str.length() - 1);
        return str;
    }

    @Override
    public @NotNull UniNode get(int index) {
        return unsupported(this, "GET_INDEX", Type.ARRAY);
    }

    @Override
    public int size() {
        return unsupported(this, "SIZE", Type.ARRAY);
    }

    @Override
    public boolean has(String fieldName) {
        return unsupported(this, "HAS_FIELD", Type.OBJECT);
    }

    @Override
    public @NotNull UniNode get(String fieldName) {
        return unsupported(this, "GET_FIELD", Type.OBJECT);
    }

    @Override
    protected void set(Object value) {
        baseReference.set(
                ValueTypeBase.STRING.convert(
                        String.valueOf(value), targetType)
        );
    }

    @Override
    public UniValueNode<T> copyFrom(@NotNull UniNode it) {
        if (it instanceof UniValueNode) {
            baseReference.set(it.as(targetType));
            return this;
        }
        return unsupported(this, "COPY_FROM_" + it.getType().name(), Type.VALUE);
    }

    @Override
    public Object asRaw(@Nullable Object fallback) {
        final String str = asString(null);

        if (str.length() == 1) {
            return asChar((char) 0);
        }

        if (str.matches("true|false")) {
            return asBoolean(false);
        }

        if (str.matches("[0-9]+")) {
            final long asLong = asLong(0);

            if (asLong > Integer.MAX_VALUE) {
                return asLong;
            } else {
                return asInt(0);
            }
        }

        if (str.matches("[0-9.]+")) {
            final double asDouble = asDouble(0);

            if (asDouble > Float.MAX_VALUE) {
                return asDouble;
            } else {
                return asFloat(0);
            }
        }

        return asString(null);
    }

    @Override
    public <R> R as(ValueType<R> type) {
        return baseReference
                .map(String::valueOf)
                .map(UniValueNode::unwrapString)
                .into(it -> ValueTypeBase.STRING.convert(it, type));
    }

    @Override
    public String asString(@Nullable String fallback) {
        if (isNull() && fallback != null) {
            return fallback;
        }

        return as(ValueTypeBase.STRING);
    }

    @Override
    public boolean asBoolean(boolean fallback) {
        if (isNull()) {
            return fallback;
        }

        return as(ValueTypeBase.BOOLEAN);
    }

    @Override
    public int asInt(int fallback) {
        if (isNull()) {
            return fallback;
        }

        return as(ValueTypeBase.INTEGER);
    }

    @Override
    public long asLong(long fallback) {
        if (isNull()) {
            return fallback;
        }

        return as(ValueTypeBase.LONG);
    }

    @Override
    public double asDouble(double fallback) {
        if (isNull()) {
            return fallback;
        }

        return as(ValueTypeBase.DOUBLE);
    }

    @Override
    public float asFloat(float fallback) {
        if (isNull()) {
            return fallback;
        }

        return as(ValueTypeBase.FLOAT);
    }

    @Override
    public short asShort(short fallback) {
        if (isNull()) {
            return fallback;
        }

        return as(ValueTypeBase.SHORT);
    }

    @Override
    public char asChar(char fallback) {
        if (isNull()) {
            return fallback;
        }

        return as(ValueTypeBase.CHARACTER);
    }

    public interface Adapter<T> extends UniNodeBase.Adapter {
        @Nullable <R> R get(ValueType<R> as);

        @Nullable String set(String value);

        final class ViaString implements Adapter<String> {
            private final Reference<String> sub;

            @Override
            public Object getBaseNode() {
                return null;
            }

            public ViaString(Reference<String> sub) {
                this.sub = sub;
            }

            @Override
            public <R> @Nullable R get(ValueType<R> as) {
                final String from = sub.get();
                if (from != null)
                    return as.getConverter().apply(from);
                return null;
            }

            @Override
            public @Nullable String set(String value) {
                sub.set(value);
                return null;
            }

            @Override
            public String toString() {
                return String.format("\"%s\"", sub.get());
            }
        }
    }
}
