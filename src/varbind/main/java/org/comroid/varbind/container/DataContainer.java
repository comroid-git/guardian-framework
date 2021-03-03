package org.comroid.varbind.container;

import org.comroid.abstr.AbstractMap;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.api.ValueBox;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.Serializable;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

public interface DataContainer<S extends DataContainer<? super S>>
        extends AbstractMap<VarBind, Object>, Serializable, ContextualProvider.Underlying {
    @Override
    default ContextualProvider getUnderlyingContextualProvider() {
        return null;
    }

    GroupBind<S> getRootBind();

    @Override
    default Object get(Object key) {
        if (key instanceof VarBind)
            //noinspection unchecked
            return getComputedReference((VarBind) key).get();
        else return getExtractionReference((String) key).get();
    }

    @Nullable
    @Override
    default Object put(String key, Object value) {
        KeyedReference<String, ReferenceIndex<Object>> ref = getExtractionReference(key);
        ReferenceIndex<Object> prev = ref.get();
        if (ref.isNull())
            ref.set(ReferenceIndex.of(value));
        else ref.computeIfPresent(refs -> {
            refs.clear();
            refs.add(value);
            return refs;
        });
        return unwrapPrev(prev);
    }

    @Override
    default Object remove(Object key) {
        final Reference ref = key instanceof VarBind
                ? getComputedReference((VarBind) key)
                : getExtractionReference((String) key);
        if (ref == null)
            return null;
        Object prev = ref.get();
        if (ref.unset())
            return unwrapPrev(prev);
        return null;
    }

    Set<VarBind<? extends S, Object, ?, Object>> updateFrom(UniObjectNode node);

    Set<VarBind<? extends S, Object, ?, Object>> initiallySet();

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

    @Deprecated
    default @NotNull <T> Reference<T> process(VarBind<? extends S, ?, ?, T> bind) {
        return getComputedReference(bind);
    }

    @Override
    default UniObjectNode toUniNode() {
        return toObjectNode(this);
    }

    default UniObjectNode toObjectNode(ContextualProvider context) {
        //noinspection unchecked
        return toObjectNode(context.requireFromContext(SerializationAdapter.class)
                .createObjectNode(context.requireFromContext(SerializationAdapter.class).getObjectType().get()));
    }

    UniObjectNode toObjectNode(UniObjectNode node);

    default <T, Value extends ValueBox<T>> @Nullable T put(VarBind<? extends S, T, ?, Value> bind, Value value) {
        if (!bind.getHeldType().equals(value.getHeldType()))
            throw new IllegalArgumentException("Unmatching ValueTypes");
        return put(bind, value.getValue());
    }

    default <T> @Nullable T put(VarBind<? extends S, T, ?, ?> bind, T value) {
        //noinspection unchecked
        return (T) put(bind.getFieldName(), value);
    }

    default <T, X> @Nullable T put(VarBind<? extends S, X, ?, T> bind, Function<T, X> parser, T value) {
        //noinspection unchecked
        return (T) put(bind, parser.apply(value));
    }

    default <E> KeyedReference<String, ReferenceIndex<E>> getExtractionReference(VarBind<?, E, ?, ?> bind) {
        return Polyfill.uncheckedCast(getExtractionReference(bind.getFieldName()));
    }

    <E> KeyedReference<String, ReferenceIndex<Object>> getExtractionReference(String name);

    <T> KeyedReference<VarBind, T> getComputedReference(VarBind<?, ?, ?, T> bind);

    @Deprecated
    default <T> KeyedReference<VarBind, Object> getComputedReference(String name) {
        return getComputedReference(getBindByName(name));
    }

    VarBind<? extends S, ?, ?, Object> getBindByName(String name);

    interface Underlying<S extends DataContainer<? super S>> extends DataContainer<S> {
        DataContainer<S> getUnderlyingVarCarrier();

        @Override
        default GroupBind<S> getRootBind() {
            return getUnderlyingVarCarrier().getRootBind();
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
        default UniObjectNode toObjectNode(UniObjectNode node) {
            return getUnderlyingVarCarrier().toObjectNode(node);
        }

        @Override
        default <T, E> @Nullable T put(VarBind<? extends S, E, ?, T> bind, Function<T, E> parser, T value) {
            return getUnderlyingVarCarrier().put(bind, parser, value);
        }

        @Override
        default <E> KeyedReference<String, ReferenceIndex<Object>> getExtractionReference(String fieldName) {
            return getUnderlyingVarCarrier().getExtractionReference(fieldName);
        }

        @Override
        default <T> KeyedReference<VarBind, Object> getComputedReference(String name) {
            return getUnderlyingVarCarrier().getComputedReference(name);
        }

        @Override
        default <T> @Nullable T put(VarBind<? extends S, T, ?, ?> bind, T value) {
            return getUnderlyingVarCarrier().put(bind, value);
        }
    }

    @Nullable
    @Internal
    @SuppressWarnings("unchecked")
    default Object unwrapPrev(Object prev) {
        if (prev != null && prev instanceof List && ((List) prev).size() == 1)
            return ((List<Object>) prev).get(0);
        return prev;
    }
}
