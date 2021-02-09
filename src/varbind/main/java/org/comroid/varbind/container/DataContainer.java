package org.comroid.varbind.container;

import org.comroid.api.*;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.Serializable;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public interface DataContainer<S extends DataContainer<? super S>> extends Map<String, Object>, Serializable, ContextualProvider.Underlying {
    GroupBind<S> getRootBind();

    Class<? extends S> getRepresentedType();

    Set<VarBind<? extends S, Object, ?, Object>> updateFrom(UniObjectNode node);

    Set<VarBind<? extends S, Object, ?, Object>> initiallySet();

    <T> Optional<Reference<T>> getByName(String name);

    default <T> @Nullable T get(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind).get();
    }

    default <T> @NotNull Optional<T> wrap(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind).wrap();
    }

    default @NotNull <T> T requireNonNull(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind).requireNonNull("No value for " + bind + " @ " + toString());
    }

    default @NotNull <T> T requireNonNull(VarBind<? extends S, ?, ?, T> bind, String message) {
        return getComputedReference(bind).requireNonNull(message);
    }

    default @NotNull <T> Processor<T> process(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind).process();
    }

    @Override
    default UniObjectNode toUniNode() {
        return toObjectNode(this);
    }

    default UniObjectNode toObjectNode(ContextualProvider context) {
        //noinspection unchecked
        return toObjectNode(context.requireFromContext(SerializationAdapter.class)
                .createUniObjectNode(context.requireFromContext(SerializationAdapter.class).getObjectType().get()));
    }

    UniObjectNode toObjectNode(UniObjectNode node);

    default <T, Value extends ValueBox<T>> @Nullable T put(VarBind<? extends S, T, ?, Value> bind, Value value) {
        if (!bind.getHeldType().equals(value.getHeldType()))
            throw new IllegalArgumentException("Unmatching ValueTypes");
        return put(bind, value.getValue());
    }

    <T> @Nullable T put(VarBind<? extends S, T, ?, ?> bind, T value);

    <T, X> @Nullable T put(VarBind<? extends S, X, ?, T> bind, Function<T, X> parser, T value);

    default <E> Reference<Span<E>> getExtractionReference(VarBind<?, E, ?, ?> bind) {
        return getExtractionReference(bind.getFieldName());
    }

    <E> Reference<Span<E>> getExtractionReference(String name);

    default <T> Reference<T> getComputedReference(VarBind<?, ?, ?, T> bind) {
        return getComputedReference(bind.getFieldName());
    }

    <T> Reference<T> getComputedReference(String name);

    interface Underlying<S extends DataContainer<? super S>> extends DataContainer<S> {
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
        default <T> Reference<T> getComputedReference(String name) {
            return getUnderlyingVarCarrier().getComputedReference(name);
        }

        @Override
        default <T> @Nullable T put(VarBind<? extends S, T, ?, ?> bind, T value) {
            return getUnderlyingVarCarrier().put(bind, value);
        }
    }
}
