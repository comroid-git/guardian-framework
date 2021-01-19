package org.comroid.uniform.model;

import org.comroid.api.ValueType;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.node.impl.StandardValueType;

import static org.comroid.api.Polyfill.uncheckedCast;
import static org.comroid.uniform.node.impl.StandardValueType.*;

public abstract class ValueAdapter<B, T> {
    protected final B base;
    private final Reference<T> actualValue = new Reference.Support.Base<T>(true) {
        @Override
        protected T doGet() {
            return asActualType();
        }

        @Override
        protected boolean doSet(T value) {
            return ValueAdapter.this.doSet(value);
        }
    };
    private final Reference<ValueType<T>> actualType = new Reference.Support.Base<ValueType<T>>(false) {
/*
        @Override
        public boolean isOutdated() {
            return atom.get() != null && actualValue.test(it -> atom.get().test(it));
        }
*/
        @Override
        protected ValueType<T> doGet() {
            return actualValue.into(StandardValueType::typeOf);
        }
    };

    public ValueType<T> getValueType() {
        return actualType.get();
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
        if (VOID.equals(type))
            return null;
        throw new UnsupportedOperationException("Unsupported Type: " + type);
    }

    public abstract T asActualType();

    protected abstract boolean doSet(T newValue);

    public boolean set(T newValue) {
        return !actualType.test(type -> type.test(newValue))
                && actualValue.set(newValue)
                && !actualType.test(type -> type.test(newValue));
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
        if (actualType.contentEquals(type))
            return uncheckedCast(asActualType());
        // need conversion
        final R result = actualType.assertion("Actual Type could not be computed").convert(asActualType(), type);
        if (result == null)
            throw new IllegalArgumentException(String.format("Cannot convert data to type: %s to %s", asActualType(), type));
        return result;
    }
}
