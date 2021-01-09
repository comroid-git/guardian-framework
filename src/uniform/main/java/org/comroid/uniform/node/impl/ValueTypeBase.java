package org.comroid.uniform.node.impl;

import org.comroid.api.Polyfill;
import org.comroid.uniform.ValueType;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public final class ValueTypeBase<R> implements ValueType<R> {
    public static final Set<ValueType<?>> values = new HashSet<>();
    public static final ValueType<Boolean> BOOLEAN
            = new ValueTypeBase<>(Boolean.class, "boolean", Boolean::parseBoolean);
    public static final ValueType<Character> CHARACTER
            = new ValueTypeBase<>(Character.class, "char", str -> str.toCharArray()[0]);
    public static final ValueType<Double> DOUBLE
            = new ValueTypeBase<>(Double.class, "double", Double::parseDouble);
    public static final ValueType<Float> FLOAT
            = new ValueTypeBase<>(Float.class, "float", Float::parseFloat);
    public static final ValueType<Integer> INTEGER
            = new ValueTypeBase<>(Integer.class, "int", Integer::parseInt);
    public static final ValueType<Long> LONG
            = new ValueTypeBase<>(Long.class, "long", Long::parseLong);
    public static final ValueType<Short> SHORT
            = new ValueTypeBase<>(Short.class, "short", Short::parseShort);
    public static final ValueType<String> STRING
            = new ValueTypeBase<>(String.class, "String", Function.identity());

    public static final ValueType<Void> VOID
            = new ValueTypeBase<>(Void.class, "Void", it -> null);

    private final Class<R> type;
    private final String name;
    private final Function<String, R> converter;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public R parse(String data) {
        return converter.apply(data);
    }

    @Override
    public Class<R> getTargetClass() {
        return type;
    }

    public ValueTypeBase(Class<R> type, String name, Function<String, R> mapper) {
        this.type = type;
        this.name = name;
        this.converter = mapper;

        values.add(this);
    }

    public static <T> ValueType<T> typeOf(T value) {
        return values.stream()
                .filter(it -> it.test(value))
                .findAny()
                .map(Polyfill::<ValueTypeBase<T>>uncheckedCast)
                .orElse(null);
    }
}
