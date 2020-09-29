package org.comroid.spellbind.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Partial {
    final class Support {
        public static <T> Optional<Class<? super T>> findPartialClass(Class<T> from) {
            if (!Modifier.isInterface(from.getModifiers())) {
                return Optional.empty();
            }

            if (from.isAnnotationPresent(Partial.class)) {
                if (!Modifier.isInterface(from.getModifiers())) {
                    throw new ClassCastException(String.format("Class %s is not an interface", from.getName()));
                } else {
                    return Optional.of(from);
                }
            } else {
                return Stream.of(from.getInterfaces())
                        .map(Support::findPartialClass)
                        .findAny()
                        .flatMap(Function.identity())
                        .map(it -> {//noinspection unchecked
                            return (Class<? super T>) it;
                        });
            }
        }
    }
}
