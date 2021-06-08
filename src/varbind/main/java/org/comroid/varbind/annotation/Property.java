package org.comroid.varbind.annotation;

import org.comroid.api.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.api.ValueType;
import org.comroid.mutatio.model.ParamRef;
import org.comroid.util.ReflectionHelper;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.exception.InvalidPropertyException;
import org.comroid.varbind.model.ConversionProperty;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Property {
    /**
     * All name variants of the property.
     * First is default value.
     *
     * @return all name variants
     */
    String[] name() default {};

    /**
     * The base type of the property.
     *
     * @return the target type of the property
     */
    Class<?> type() default Void.class;

    /**
     * The converted type of the property.
     *
     * @return the target type of the property
     */
    Class<?> converted() default Void.class;

    /**
     * The field containing the type definition, if not resolvable by {@link #type()}.
     * @return the field containing the type definition
     */
    String typedef() default "";

    /**
     * Whether the property is required in data.
     *
     * @return whether the property is required in data
     */
    boolean required() default false;

    /**
     * Whether the data of the property may be null.
     * If {@code false}, a {@linkplain java.util.Objects#isNull(Object) null check} needs to be performed.
     *
     * @return whether the data may be null
     */
    boolean nullable() default true;

    /**
     * Whether the property contains an array of the {@linkplain #type() target type} of the property.
     *
     * @return whether the property contains an array of data
     */
    boolean array() default false;

    /**
     * Whether the property is read-only.
     * @return whether the property is read-only
     */
    boolean readonly() default true;

    /**
     * Whether the property contains the identifier data of the object.
     * Only one property per object may contain the identifier.
     *
     * @return whether the property contains the identifier data of the object
     */
    boolean identifier() default false;

    /*
    /**
     * Whether to enforce reflective accessibility of the representation field.
     *
     * @return whether to enforce reflective accessibility
    boolean forceAccessible() default false;
    */

    /**
     * The format of the setter method.
     * The letter {@code &} represents the name of the property.
     *
     * @return the format of the setter method
     */
    String setter() default "set&";

    /**
     * The format of the setter method.
     * The letter {@code &} represents the name of the property.
     *
     * @return the format of the setter method
     */
    String converter() default "convert&";

    final class Support {
        private static final Map<Property, ConversionProperty<?, ?, ?>> cache = new ConcurrentHashMap<>();

        public static <T, I, O> List<ConversionProperty<T, I, O>> findProperties(Class<T> inClass) {
            //noinspection Convert2MethodRef
            return Stream.concat(
                    Arrays.stream(inClass.getDeclaredFields()),
                    Arrays.stream(inClass.getDeclaredMethods()))
                    .filter(member -> member.isAnnotationPresent(Property.class))
                    .map(source -> Support.<T, I, O>impl(source))
                    .collect(Collectors.toList());
        }

        private static <T, I, O> ConversionProperty<T, I, O> impl(AccessibleObject source) {
            Property prop = source.getAnnotation(Property.class);
            //noinspection unchecked
            return (ConversionProperty<T, I, O>) cache.computeIfAbsent(prop, k -> new Impl<T, I, O>(source, k));
        }

        @SuppressWarnings({"rawtypes", "FieldMayBeFinal", "unchecked"})
        private static final class Impl<OWNER, BASE, CONVERT> implements ConversionProperty<OWNER, BASE, CONVERT> {
            private final Property prop;
            private final List<String> names;
            private final ValueType<BASE> targetType;
            private final Invocable<CONVERT> converter;
            private final Invocable getter;
            private BiPredicate<OWNER, Object> setter;
            private boolean mutable;

            @Override
            public ParamRef<OWNER, CONVERT> convert() {
                return mapBoth();
            }

            @Override
            public String getName() {
                return names.get(0);
            }

            @Override
            public List<String> getNameAlternatives() {
                return names;
            }

            @Override
            public ValueType<BASE> getTargetType() {
                return targetType;
            }

            @Override
            public Class<CONVERT> getConvertedType() {
                return (Class<CONVERT>) converted();
            }

            @Override
            public boolean isRequired() {
                return required();
            }

            @Override
            public boolean isNullable() {
                return nullable();
            }

            @Override
            public boolean isArray() {
                return array();
            }

            @Override
            public boolean isIdentifier() {
                return identifier();
            }

            @Override
            public boolean isMutable() {
                return !readonly() || (setter != null && mutable);
            }

            @Override
            public boolean setMutable(boolean state) {
                return mutable;
            }

            @Override
            public BASE get(OWNER param) {
                try {
                    return (BASE) getter.invoke(param);
                } catch (InvocationTargetException | IllegalAccessException | InstantiationException e) {
                    throw new RuntimeException("Could not access property getter", e);
                }
            }

            @Override
            public boolean set(OWNER param, BASE value) {
                return setter.test(param, value);
            }

            private Impl(AccessibleObject source, Property prop) {
                this.prop = prop;
                this.names = prop.name().length == 0
                        ? Collections.singletonList(((Member) source).getName())
                        : Stream.concat(
                        Arrays.stream(prop.name()),
                        Stream.of(((Member) source).getName())
                ).collect(Collectors.toList());
                this.targetType = (ValueType<BASE>) findTargetType(source, prop);
                this.getter = makeGetter(source);
                this.setter = readonly() ? null : makeSetter(source);
                this.converter = makeConverter();
                this.mutable = !readonly();
            }

            private Invocable<CONVERT> makeConverter() {
                String converter = converter();
                converter = converter.replace("&", getName());
                return null;
            }

            private static ValueType<?> findTargetType(AccessibleObject source, Property prop) {
                Class<?> cls = null;
                if (source instanceof Field)
                    cls = ((Field) source).getType();
                if (source instanceof Method)
                    cls = ((Method) source).getReturnType();
                assert cls != null : "unreachable";
                return StandardValueType.forClass(cls)
                        .or(() -> ReflectionHelper.resolveField(prop.typedef()))
                        .ifPresentMapOrElseThrow(
                                Polyfill::uncheckedCast,
                                () -> new InvalidPropertyException("Could not resolve ValueType of property: " + source));
            }

            private static Invocable makeGetter(AccessibleObject source) {
                class FieldGetter implements Invocable {
                    private final Field field;

                    FieldGetter(AccessibleObject accessible) {
                        this.field = (Field) accessible;
                    }

                    @Override
                    public Class<?>[] parameterTypesOrdered() {
                        return new Class[0];
                    }

                    @Nullable
                    @Override
                    public Object invoke(@Nullable Object target, Object... args) throws IllegalAccessException {
                        return field.get(target);
                    }
                }

                if (source instanceof Field)
                    return new FieldGetter(source);
                if (source instanceof Method)
                    return Invocable.ofMethodCall((Method) source);
                throw new AssertionError("unreachable");
            }

            private BiPredicate<OWNER, Object> makeSetter(AccessibleObject source) {
                class FieldSetter implements BiPredicate<OWNER, Object> {
                    private final Field field;

                    public FieldSetter(AccessibleObject source) {
                        this.field = (Field) source;
                    }

                    @Override
                    public boolean test(OWNER owner, Object value) {
                        try {
                            field.set(owner, value);
                            return true;
                        } catch (IllegalAccessException e) {
                            throw new AssertionError("Could not set field", e);
                        }
                    }
                }

                if (source instanceof Field)
                    return new FieldSetter(source);
                return null; // todo
            }

            @Override
            public String[] name() {
                return names.toArray(new String[0]);
            }

            @Override
            public Class<?> type() {
                return targetType.getTargetClass();
            }

            @Override
            public Class<?> converted() {
                return prop.converted();
            }

            @Override
            public String typedef() {
                return prop.typedef();
            }

            @Override
            public boolean required() {
                return prop.required();
            }

            @Override
            public boolean nullable() {
                return prop.nullable();
            }

            @Override
            public boolean array() {
                return prop.array();
            }

            @Override
            public boolean readonly() {
                return prop.readonly();
            }

            @Override
            public boolean identifier() {
                return prop.identifier();
            }

            /*
            @Override
            public boolean forceAccessible() {
                return prop.forceAccessible();
            }
             */

            @Override
            public String setter() {
                return prop.setter();
            }

            @Override
            public String converter() {
                return prop.converter();
            }

            @Override
            public Class<? extends Annotation> annotationType() {
                return Property.class;
            }
        }
    }
}
