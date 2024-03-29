package org.comroid.uniform.model;

import org.comroid.api.ValueType;
import org.comroid.mutatio.ref.Reference;
import org.comroid.util.StandardValueType;

import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.util.StandardValueType.*;

public abstract class ValueAdapter<B, T> {
    protected final B base;
    private final Reference<T> actualValue = new Reference<>(this::asActualType, this::doSet);

    public ValueType<T> getValueType() {
        return actualValue.into(StandardValueType::typeOf);
    }

    public B getBase() {
        return base;
    }

    protected ValueAdapter(B base) {
        this.base = base;
    }

    public final <R> R asType(ValueType<R> type) {
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
        if (OBJECT.equals(type))
            return uncheckedCast(asActualType());
        if (VOID.equals(type))
            return null;
        throw new UnsupportedOperationException("Unsupported Type: " + type.getName());
    }

    public abstract T asActualType();

    protected abstract boolean doSet(T newValue);

    public boolean set(T newValue) {
        return getValueType().test(newValue) && actualValue.set(newValue);
    }

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
        if (getValueType().equals(type))
            return uncheckedCast(asActualType());
        // need conversion
        final R result = getValueType().convert(asActualType(), type);
        if (result == null)
            throw new IllegalArgumentException(String.format("Cannot convert data to type: %s to %s", asActualType(), type));
        return result;
    }
}
