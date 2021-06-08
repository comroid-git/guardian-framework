package org.comroid.varbind.model;

import org.comroid.api.ValueType;
import org.comroid.mutatio.model.ParamRef;
import org.comroid.varbind.annotation.Prop;

import java.util.List;

@SuppressWarnings("ClassExplicitlyAnnotation")
public interface Property<T> extends Prop, ParamRef<T, Object> {
    List<String> getNameAlternatives();

    ValueType<?> getTargetType();

    boolean isRequired();

    boolean isNullable();

    boolean isArray();

    boolean isIdentifier();

    boolean isForceAccessible();
}
