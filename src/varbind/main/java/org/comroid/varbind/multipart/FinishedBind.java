package org.comroid.varbind.multipart;

import org.comroid.api.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.api.UUIDContainer;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.span.Span;
import org.comroid.spellbind.model.TypeFragmentProvider;

import java.util.Collection;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class FinishedBind {
    public static <R> TypeFragmentProvider<PartialBind.Finisher<R, R>> singleResultProvider() {
        return new FragmentProviders.SingleResult<>();
    }

    public static <R, C extends Collection<R>> TypeFragmentProvider<PartialBind.Finisher<R, C>> collectingProvider() {
        return new FragmentProviders.IntoCollection<>();
    }

    public static final class SingleResult<R> extends UUIDContainer.Base implements PartialBind.Finisher<R, R> {
        private static final Invocable<? super SingleResult<?>> constructor = Invocable.ofConstructor(SingleResult.class);

        @Override
        public boolean isListing() {
            return false;
        }

        @Override
        public R finish(Span<R> parts) {
            return as(PartialBind.Base.class)
                    .map(base -> base.isRequired()
                            ? parts.requireNonNull(MessageSupplier.format("Could not find value for bind %s", base.getFieldName()))
                            : parts.get())
                    .orElseGet(parts);
        }
    }

    public static class IntoCollection<R, C extends Collection<R>> extends UUIDContainer.Base implements PartialBind.Finisher<R, C> {
        private static final Invocable<? super IntoCollection<?, ?>> constructor = Invocable.ofConstructor(IntoCollection.class);

        private final Supplier<C> collectionSupplier;

        @Override
        public boolean isListing() {
            return true;
        }

        public IntoCollection(Supplier<C> collectionSupplier) {
            this.collectionSupplier = collectionSupplier;
        }

        @Override
        public C finish(Span<R> parts) {
            if (collectionSupplier != null)
                return parts.stream().collect(Collectors.toCollection(collectionSupplier));
            return Polyfill.uncheckedCast(parts);
        }
    }

    private static final class FragmentProviders {
        private interface FinisherProvider<R, F> extends TypeFragmentProvider<PartialBind.Finisher<R, F>> {
            @Override
            default Class<PartialBind.Finisher<R, F>> getInterface() {
                return Polyfill.uncheckedCast(PartialBind.Finisher.class);
            }
        }

        private static final class SingleResult<R> implements FinisherProvider<R, R> {
            @Override
            public Invocable.TypeMap<? extends PartialBind.Finisher<R, R>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(FinishedBind.SingleResult.constructor.typeMapped());
            }
        }

        private static final class IntoCollection<R, C extends Collection<R>> implements FinisherProvider<R, C> {
            @Override
            public Invocable.TypeMap<? extends PartialBind.Finisher<R, C>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(FinishedBind.IntoCollection.constructor.typeMapped());
            }
        }
    }
}
