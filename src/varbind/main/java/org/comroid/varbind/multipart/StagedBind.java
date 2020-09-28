package org.comroid.varbind.multipart;

import org.comroid.api.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.api.UUIDContainer;
import org.comroid.spellbind.model.TypeFragmentProvider;

import java.util.function.BiFunction;
import java.util.function.Function;

public final class StagedBind {
    public static <T> TypeFragmentProvider<? super OneStage<T>> oneStageProvider() {
        return new FragmentProviders.OneStage<>();
    }

    public static <T, R> TypeFragmentProvider<? super TwoStage<T, R>> twoStageProvider() {
        return new FragmentProviders.TwoStage<>();
    }

    public static <T, D, R> TypeFragmentProvider<? super DependentTwoStage<T, D, R>> dependentTwoStageProvider() {
        return new FragmentProviders.DependentTwoStage<>();
    }

    public static final class OneStage<T> extends UUIDContainer.Base implements PartialBind.Remapper<Object, T, T> {
        private static final Invocable<? super OneStage<?>> constructor = Invocable.ofConstructor(OneStage.class);

        @Override
        public T remap(Object dependency, T from) {
            return from;
        }
    }

    public static final class TwoStage<T, R> extends UUIDContainer.Base implements PartialBind.Remapper<Object, T, R> {
        private static final Invocable<? super TwoStage<?, ?>> constructor = Invocable.ofConstructor(TwoStage.class);
        private final Function<T, R> remapper;

        public TwoStage(Function<T, R> remapper) {
            this.remapper = remapper;
        }

        @Override
        public R remap(Object dependency, T from) {
            return remapper.apply(from);
        }
    }

    public static final class DependentTwoStage<T, D, R> extends UUIDContainer.Base implements PartialBind.Remapper<D, T, R> {
        private static final Invocable<? super DependentTwoStage<?, ?, ?>> constructor = Invocable.ofConstructor(DependentTwoStage.class);
        private final BiFunction<T, D, R> resolver;

        public DependentTwoStage(BiFunction<T, D, R> resolver) {
            this.resolver = resolver;
        }

        @Override
        public R remap(D dependency, T from) {
            if (dependency == null)
                throw new NullPointerException("Dependency Object is null");

            return resolver.apply(from, dependency);
        }
    }

    private static final class FragmentProviders {
        private interface RemapperProvider<T, D, R> extends TypeFragmentProvider<PartialBind.Remapper<D, T, R>> {
            @Override
            default Class<PartialBind.Remapper<D, T, R>> getInterface() {
                return Polyfill.uncheckedCast(PartialBind.Remapper.class);
            }
        }

        private static final class OneStage<T> implements RemapperProvider<T, Object, T> {
            @Override
            public Invocable.TypeMap<? extends PartialBind.Remapper<Object, T, T>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(StagedBind.OneStage.constructor.typeMapped());
            }
        }

        private static final class TwoStage<T, R> implements RemapperProvider<T, Object, R> {
            @Override
            public Invocable.TypeMap<? extends PartialBind.Remapper<Object, T, R>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(StagedBind.TwoStage.constructor.typeMapped());
            }
        }

        private static final class DependentTwoStage<T, D, R> implements RemapperProvider<T, D, R> {
            @Override
            public Invocable.TypeMap<? extends PartialBind.Remapper<D, T, R>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(StagedBind.DependentTwoStage.constructor.typeMapped());
            }
        }
    }
}
