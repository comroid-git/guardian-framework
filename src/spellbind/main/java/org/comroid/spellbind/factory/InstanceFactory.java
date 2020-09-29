package org.comroid.spellbind.factory;

import org.comroid.api.Invocable;
import org.comroid.common.info.Dependent;
import org.comroid.mutatio.ref.OutdateableReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.spellbind.SpellCore;
import org.comroid.spellbind.model.TypeFragment;
import org.comroid.spellbind.model.TypeFragmentProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public final class InstanceFactory<T extends TypeFragment<? super T>, D>
        implements Invocable.TypeMap<T>, Dependent<D> {
    private final Reference<Class<?>[]> requiredTypes = Reference.create();
    private final Class<T> mainInterface;
    private final Reference<? extends T> coreObject;
    private final @Nullable D dependent;
    private final Collection<TypeFragmentProvider<? super T>> typeFragmentProviders;

    @Override
    public @Nullable D getDependent() {
        return dependent;
    }

    @SafeVarargs
    public InstanceFactory(
            Class<T> mainInterface,
            Reference<? extends T> coreObject,
            @Nullable D dependent,
            TypeFragmentProvider<? super T>... typeFragmentProviders
    ) {
        this.mainInterface = mainInterface;
        this.coreObject = coreObject;
        this.dependent = dependent;
        this.typeFragmentProviders = Arrays.asList(typeFragmentProviders);

        final Class<?>[] classes = parameterTypesOrdered();
        if (classes.length != streamParamTypes().count())
            throw new IllegalArgumentException("Duplicate parameter type detected: " + Arrays.toString(classes));
    }

    public void addSubimplementation(TypeFragmentProvider<? super T> fragmentProvider) {
        typeFragmentProviders.add(fragmentProvider);
        requiredTypes.outdate();
    }

    @Override
    public Class<?>[] parameterTypesOrdered() {
        return requiredTypes.compute(arr -> streamParamTypes().distinct().toArray(Class[]::new));
    }

    @NotNull
    private Stream<Class<?>> streamParamTypes() {
        return Stream.concat(
                dependent == null ? Stream.empty() : Stream.of(dependent.getClass()),
                this.typeFragmentProviders.stream()
                        .map(TypeFragmentProvider::getInstanceSupplier)
                        .map(Invocable::parameterTypesOrdered)
                        .flatMap(Arrays::stream)
        );
    }

    @Nullable
    @Override
    public T invoke(Map<Class<?>, Object> args) {
        final SpellCore.Builder<T> builder = SpellCore.builder(mainInterface, coreObject.requireNonNull("Core Object"));

        if (dependent != null)
            args.putIfAbsent(dependent.getClass(), dependent);
        typeFragmentProviders.forEach(builder::addFragment);

        return builder.build(args);
    }
}
