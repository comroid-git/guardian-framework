package org.comroid.spellbind;

import org.comroid.api.*;
import org.comroid.common.exception.AssertionException;
import org.comroid.spellbind.model.TypeFragment;
import org.comroid.spellbind.model.TypeFragmentProvider;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SpellCore<T extends TypeFragment<? super T>>
        extends UUIDContainer.Base
        implements TypeFragment<T>, InvocationHandler {
    static final Map<UUID, SpellCore<?>> coreInstances = new ConcurrentHashMap<>();
    private final CompletableFuture<T> proxyFuture = new CompletableFuture<>();
    private final Object base;
    private final Map<String, Invocable<?>> methods;

    private SpellCore(Object base, Map<String, Invocable<?>> methods) {
        this.base = base;
        this.methods = Collections.unmodifiableMap(methods);
    }

    public static <T extends TypeFragment<? super T>> Builder<T> builder(Class<T> mainInterface) {
        class Local extends Base implements TypeFragment<T> {
        }

        return builder(mainInterface, new Local());
    }

    public static <T extends TypeFragment<? super T>> SpellCore.Builder<T> builder(Class<T> mainInterface, TypeFragment<? super T> base) {
        return new Builder<>(mainInterface, base);
    }

    @Internal
    public static <T extends TypeFragment<? super T>> Optional<SpellCore<T>> getCore(TypeFragment<?> ofFragment) {
        final UUID uuid = ofFragment.getUUID();

        if (!coreInstances.containsKey(uuid))
            return Optional.empty();
        return Optional.of(coreInstances.get(uuid))
                .map(Polyfill::uncheckedCast);
    }

    public static String methodString(Method method) {
        return String.format(
                "%s(%s): %s",
                method.getName(),
                Arrays.stream(method.getParameterTypes())
                        .map(Class::getCanonicalName)
                        .collect(Collectors.joining(",")),
                method.getReturnType().getCanonicalName()
        );
    }

    @Override
    public T self() {
        return proxyFuture.join();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (!Modifier.isAbstract(method.getModifiers())
                && method.getDeclaringClass().isAssignableFrom(base.getClass()))
            return Invocable.ofMethodCall(base, method)
                    .invokeRethrow(args);

        String methodString = methodString(method);

        if (method.getName().equals("self") && method.getParameterCount() == 0)
            return self();

        Invocable<?> invocable = methods.get(methodString);

        if (invocable != null)
            return invocable.invokeRethrow((ReflectiveOperationException e) -> new RuntimeException(e.getCause()), args);

        throw new NoSuchMethodError("No implementation found for " + methodString);
    }

    public static final class Builder<T extends TypeFragment<? super T>> {
        private final TypeFragment<? super T> base;
        private final Collection<TypeFragmentProvider<? super T>> typeFragmentProviders = new ArrayList<>();
        private final Set<Class<? super T>> interfaces = new HashSet<>();
        private ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        public Builder<T> setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public Builder(Class<T> mainInterface, TypeFragment<? super T> base) {
            this.base = base;
            interfaces.add(mainInterface);
        }

        public Builder<T> addFragment(TypeFragmentProvider<? super T> provider) {
            typeFragmentProviders.add(provider);
            return this;
        }

        public T build(Object... args) {
            final Map<String, Invocable<?>> methods = new ConcurrentHashMap<>();

            scanMethods(methods, base);

            final Set<? extends TypeFragment<? super T>> fragments = typeFragmentProviders.stream()
                    .map(provider -> resolveTypeFragmentProvider(methods, provider, args))
                    .collect(Collectors.toSet());
            final SpellCore<T> spellCore = new SpellCore<>(base, methods);
            final T proxy = Polyfill.uncheckedCast(Proxy.newProxyInstance(
                    classLoader,
                    interfaces.toArray(new Class[0]),
                    spellCore
            ));
            spellCore.proxyFuture.complete(proxy);
            SpellCore.coreInstances.put(base.getUUID(), spellCore);
            fragments.forEach(it -> SpellCore.coreInstances
                    .put(it.getUUID(), spellCore));

            return proxy;
        }

        private TypeFragment<? super T> resolveTypeFragmentProvider(
                Map<String, Invocable<?>> intoMap,
                TypeFragmentProvider<? super T> provider,
                Object[] args
        ) {
            final TypeFragment<? super T> fragment = provider.getInstanceSupplier().autoInvoke(args);
            final Class<? super T> target = provider.getInterface();

            if (!target.isInterface())
                throw new IllegalArgumentException("Can only implement interfaces as TypeFragments");
            interfaces.add(target);

            scanMethods(intoMap, fragment);

            return fragment;
        }

        private void scanMethods(Map<String, Invocable<?>> intoMap, Object fragment) {
            Arrays.stream(fragment.getClass().getMethods())
                    .filter(method -> !method.getDeclaringClass().equals(Object.class))
                    .forEach(method -> {
                        final Invocable<?> invocable = buildInvocable(method, fragment);
                        final String methodString = methodString(method);

                        intoMap.put(methodString, invocable);
                    });
        }

        private Invocable<?> buildInvocable(Method method, @Nullable Object target) {
            if (Modifier.isStatic(method.getModifiers()))
                return Invocable.ofMethodCall(method);
            else return target == null
                    ? Invocable.ofMethodCall(base, method)
                    : Invocable.ofMethodCall(target, method);
        }
    }
}
