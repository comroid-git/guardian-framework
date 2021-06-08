package org.comroid.varbind.annotation;

import org.comroid.api.Polyfill;
import org.comroid.api.ValueType;
import org.comroid.util.ReflectionHelper;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.exception.InvalidPropertyException;
import org.comroid.varbind.model.Property;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Prop {
    /**
     * All name variants of the property.
     * First is default value.
     *
     * @return all name variants
     */
    String[] name() default {};

    /**
     * The target type of the property.
     *
     * @return the target type of the property
     */
    Class<?> type() default Void.class;

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
     * Whether the property contains the identifier data of the object.
     * Only one property per object may contain the identifier.
     *
     * @return whether the property contains the identifier data of the object
     */
    boolean identifier() default false;

    /**
     * Whether to enforce reflective accessibility of the representation field.
     *
     * @return whether to enforce reflective accessibility
     */
    boolean forceAccessible() default false;

    /**
     * The format of the setter method.
     * The letter {@code &} represents the name of the property.
     *
     * @return the format of the setter method
     */
    String setter() default "set&";

    final class Support {
        private static final Map<Prop, Property<?>> cache = new ConcurrentHashMap<>();

        public static <T> List<Property<T>> findProperties(Class<T> inClass) {
            //noinspection Convert2MethodRef
            return Stream.concat(
                    Arrays.stream(inClass.getDeclaredFields()),
                    Arrays.stream(inClass.getDeclaredMethods()))
                    .filter(member -> member.isAnnotationPresent(Prop.class))
                    .map(source -> Support.<T>impl(source))
                    .collect(Collectors.toList());
        }

        private static <T> Property<T> impl(AccessibleObject source) {
            Prop prop = source.getAnnotation(Prop.class);
            //noinspection unchecked
            return (Property<T>) cache.computeIfAbsent(prop, k -> new Impl<T>(source, k));
        }

        private static final class Impl<OWNER> implements Property<OWNER> {
            private final AccessibleObject accessible;
            private final Member member;
            private final Prop prop;
            private final List<String> names;
            private final ValueType<?> targetType;

            @Override
            public List<String> getNameAlternatives() {
                return names;
            }

            @Override
            public ValueType<?> getTargetType() {
                return targetType;
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
            public boolean isForceAccessible() {
                return forceAccessible();
            }

            private Impl(AccessibleObject source, Prop prop) {
                this.accessible = source;
                this.member = (Member) source;
                this.prop = prop;
                this.names = prop.name().length == 0
                        ? Collections.singletonList(member.getName())
                        : Stream.concat(
                        Arrays.stream(prop.name()),
                        Stream.of(member.getName())
                ).collect(Collectors.toList());
                this.targetType = findTargetType(source, prop);
            }

            private static ValueType<?> findTargetType(AccessibleObject source, Prop prop) {
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
                                () -> new InvalidPropertyException("Could not resolve ValueType of property: " + source)
                        );
            }
        }
    }
}
