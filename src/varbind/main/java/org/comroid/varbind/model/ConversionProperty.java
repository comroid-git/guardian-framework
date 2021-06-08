package org.comroid.varbind.model;

import org.comroid.api.Named;
import org.comroid.api.ValueType;
import org.comroid.mutatio.model.ParamRef;
import org.comroid.varbind.annotation.Property;

import java.util.List;

@SuppressWarnings("ClassExplicitlyAnnotation")
public interface ConversionProperty<P, I, O> extends Property, ParamRef<P, I>, Named {
    ParamRef<P, O> convert();

    List<String> getNameAlternatives();

    ValueType<I> getTargetType();

    Class<O> getConvertedType();

    boolean isRequired();

    boolean isNullable();

    boolean isArray();

    boolean isIdentifier();

    //boolean isForceAccessible();
}
