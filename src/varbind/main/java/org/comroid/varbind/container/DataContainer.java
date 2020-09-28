package org.comroid.varbind.container;

import org.comroid.api.SelfDeclared;
import org.comroid.mutatio.proc.Processor;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public interface DataContainer<S extends DataContainer<? super S> & SelfDeclared<? super S>> extends SelfDeclared<S> {
    GroupBind<S> getRootBind();

    Class<? extends S> getRepresentedType();

    Set<VarBind<? extends S, Object, ?, Object>> updateFrom(UniObjectNode node);

    Set<VarBind<? extends S, Object, ?, Object>> initiallySet();

    <T> Optional<Reference<T>> getByName(String name);

    @Deprecated
    default <T> @NotNull Reference<T> ref(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind);
    }

    default <T> @Nullable T get(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind).get();
    }

    default <T> @NotNull Optional<T> wrap(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind).wrap();
    }

    default @NotNull <T> T requireNonNull(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind).requireNonNull("No value for property " + bind.getFieldName());
    }

    default @NotNull <T> T requireNonNull(VarBind<? extends S, ?, ?, T> bind, String message) {
        return getComputedReference(bind).requireNonNull(message);
    }

    default @NotNull <T> Processor<T> process(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind).process();
    }

    default UniObjectNode toObjectNode(SerializationAdapter<?, ?, ?> serializationAdapter) {
        return toObjectNode(serializationAdapter.createUniObjectNode(null));
    }

    UniObjectNode toObjectNode(UniObjectNode node);

    <T> @Nullable T put(VarBind<? extends S, T, ?, ?> bind, T value);

    <T, X> @Nullable T put(VarBind<? extends S, X, ?, T> bind, Function<T, X> parser, T value);

    <E> Reference<Span<E>> getExtractionReference(String fieldName);

    default <E> Reference<Span<E>> getExtractionReference(VarBind<? extends S, E, ?, ?> bind) {
        return getExtractionReference(cacheBind(bind));
    }

    <T, E> Reference<T> getComputedReference(VarBind<? extends S, E, ?, T> bind);

    @Internal
    <T> String cacheBind(VarBind<? extends S, ?, ?, ?> bind);

    interface Underlying<S extends DataContainer<? super S> & SelfDeclared<? super S>> extends DataContainer<S> {
        DataContainer<S> getUnderlyingVarCarrier();

        @Override
        default GroupBind<S> getRootBind() {
            return getUnderlyingVarCarrier().getRootBind();
        }

        @Override
        default Class<? extends S> getRepresentedType() {
            return getUnderlyingVarCarrier().getRepresentedType();
        }

        @Override
        default Set<VarBind<? extends S, Object, ?, Object>> updateFrom(UniObjectNode node) {
            return getUnderlyingVarCarrier().updateFrom(node);
        }

        @Override
        default Set<VarBind<? extends S, Object, ?, Object>> initiallySet() {
            return getUnderlyingVarCarrier().initiallySet();
        }

        @Override
        default <T> Optional<Reference<T>> getByName(String name) {
            return getUnderlyingVarCarrier().getByName(name);
        }

        @Override
        default UniObjectNode toObjectNode(UniObjectNode node) {
            return getUnderlyingVarCarrier().toObjectNode(node);
        }

        @Override
        default <T, E> @Nullable T put(VarBind<? extends S, E, ?, T> bind, Function<T, E> parser, T value) {
            return getUnderlyingVarCarrier().put(bind, parser, value);
        }

        @Override
        default <E> Reference<Span<E>> getExtractionReference(String fieldName) {
            return getUnderlyingVarCarrier().getExtractionReference(fieldName);
        }

        @Override
        default <T, E> Reference<T> getComputedReference(VarBind<? extends S, E, ?, T> bind) {
            return getUnderlyingVarCarrier().getComputedReference(bind);
        }

        @Override
        default <T> String cacheBind(VarBind<? extends S, ?, ?, ?> bind) {
            return getUnderlyingVarCarrier().cacheBind(bind);
        }

        @Override
        default <T> @Nullable T put(VarBind<? extends S, T, ?, ?> bind, T value) {
            return getUnderlyingVarCarrier().put(bind, value);
        }
    }
}
