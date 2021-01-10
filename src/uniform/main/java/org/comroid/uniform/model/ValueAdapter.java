package org.comroid.uniform.model;

import org.comroid.api.ContextualProvider;
import org.comroid.api.HeldType;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;

import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.uniform.node.impl.StandardValueType.*;

public abstract class ValueAdapter<B, T> implements ContextualProvider.Member {
    protected final SerializationAdapter<? super B, ?, ?> seriLib;
    protected final ValueType<T> actualType;
    protected final B base;

    @Override
    public SerializationAdapter<? super B, ?, ?> getFromContext() {
        return seriLib;
    }

    public ValueType<T> getValueType() {
        return actualType;
    }

    public B getBase() {
        return base;
    }

    protected ValueAdapter(SerializationAdapter<? super B, ?, ?> seriLib, ValueType<T> actualType, B base) {
        this.seriLib = seriLib;
        this.actualType = actualType;
        this.base = base;
    }

    public final <R> R asType(HeldType<R> type) {
        if (STRING.equals(type))
            return uncheckedCast(asString());
        if (CHARACTER.equals(type))
            return uncheckedCast(asChar());
        if (INTEGER.equals(type))
            return uncheckedCast(asInt());
        if (BOOLEAN.equals(type))
            return uncheckedCast(asBoolean());
        if (LONG.equals(type))
            return uncheckedCast(asLong());
        if (DOUBLE.equals(type))
            return uncheckedCast(asDouble());
        if (FLOAT.equals(type))
            return uncheckedCast(asFloat());
        if (SHORT.equals(type))
            return uncheckedCast(asShort());
        if (VOID.equals(type))
            return null;
        return actualType.convert(asActualType(), type);
    }

    public abstract T asActualType();

    public final String asString() {
        return returnAsType(STRING);
    }

    public final Character asChar() {
        return returnAsType(CHARACTER);
    }

    public final Boolean asBoolean() {
        return returnAsType(BOOLEAN);
    }

    public final Integer asInt() {
        return returnAsType(INTEGER);
    }

    public final Long asLong() {
        return returnAsType(LONG);
    }

    public final Double asDouble() {
        return returnAsType(DOUBLE);
    }

    public final Float asFloat() {
        return returnAsType(FLOAT);
    }

    public final Short asShort() {
        return returnAsType(SHORT);
    }

    private <R> R returnAsType(ValueType<R> type) {
        if (type.equals(VOID))
            return null;
        if (type.equals(actualType))
            return uncheckedCast(asActualType());
        final R result = asType(type);
        if (result == null)
            throw new IllegalArgumentException(String.format("Cannot convert data to type: %s to %s", asActualType(), type));
        return result;
    }
}
