package org.comroid.varbind.container;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.ReferenceAtlas;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.ref.ReferenceMap;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DataContainerBase<S extends DataContainer<? super S>>
        extends ReferenceAtlas<String, VarBind, ReferenceIndex, Object
        , KeyedReference<String, ReferenceIndex>, DataContainerBase.OutputReference>
        implements DataContainer<S> {
    private final ContextualProvider context;
    private final GroupBind<S> group;
    private final Set<VarBind<? extends S, Object, ?, Object>> initialValues;

    @Override
    public final GroupBind<S> getRootBind() {
        return group;
    }

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    protected DataContainerBase(
            ContextualProvider context,
            final GroupBind<S> group,
            @Nullable Comparator<OutputReference> comparator,
            UniObjectNode initialData
    ) {
        super(
                new ReferenceMap<>(),
                null,
                Polyfill.uncheckedCast(comparator),
                name -> group.streamAllChildren()
                        .filter(vb -> vb.getFieldName().equals(name))
                        .findFirst()
                        .orElse(null),
                VarBind::getFieldName
        );
        this.context = context;
        this.group = group;
        this.initialValues = updateFrom(initialData);
    }

    @Override
    public final Set<VarBind<? extends S, Object, ?, Object>> updateFrom(UniObjectNode node) {
        final Set<VarBind<? extends S, Object, ?, Object>> initialized = new HashSet<>();
        node.forEach((key, value) -> {
            KeyedReference<String, ReferenceIndex<?, Object>> eRef = getExtractionReference(key);
            VarBind<? extends S, ?, ?, Object> bind = getBindByName(key);

            eRef.compute(refs -> {
                if (refs == null)
                    refs = new ReferenceIndex<>();
                else refs.clear();
                refs.add(value);
                return refs;
            });
            if (bind != null) {
                KeyedReference<VarBind, Object> cRef = getComputedReference(bind);
                Object prev = cRef.get();
                if (cRef != null && cRef.get() != prev)
                    initialized.add(Polyfill.uncheckedCast(bind));
            }
        });
        return Collections.unmodifiableSet(initialized);
    }

    @Override
    public final Set<VarBind<? extends S, Object, ?, Object>> initiallySet() {
        return initialValues;
    }

    @Override
    public final UniObjectNode toObjectNode(final UniObjectNode node) {
        parent.streamRefs()
                .forEach(ref -> {
                    String field = ref.getKey();
                    ReferenceIndex values = ref.orElseGet(Span::empty);

                    if (values.size() == 1)
                        node.put(field, values.get(0));
                    else if (values.size() == 0)
                        node.putNull(field);
                    else {
                        UniArrayNode arr = node.putArray(field);
                        values.forEach(arr::addValue);
                    }
                });
        return node;
    }

    @Override
    public final <E> KeyedReference<String, ReferenceIndex<?, Object>> getExtractionReference(String name) {
        return Polyfill.uncheckedCast(getInputReference(name, true));
    }

    @Override
    public final <T> KeyedReference<VarBind, T> getComputedReference(VarBind<?, ?, ?, T> bind) {
        return Polyfill.uncheckedCast(getReference(bind, true));
    }

    @Override
    public final VarBind<? extends S, ?, ?, Object> getBindByName(String name) {
        //noinspection unchecked
        return keyAdvancer.apply(name);
    }

    @Override
    protected final OutputReference createEmptyRef(VarBind bind) {
        KeyedReference<String, ReferenceIndex> base = getInputReference(bind.getFieldName(), true);
        if (base == null)
            throw new NullPointerException("Could not create base reference");
        return new OutputReference(bind, base);
    }

    @Override
    protected final OutputReference advanceReference(KeyedReference<String, ReferenceIndex> inputRef) {
        String key = inputRef.getKey();
        VarBind bind = keyAdvancer.apply(key);
        return new OutputReference(bind, inputRef);
    }

    private <T, R> R computeValueFor(VarBind<? super S, T, ?, R> bind, ReferenceIndex<?, T> fromBase) {
        for (VarBind<?, ?, ?, ?> dependency : bind.getDependencies())
            getExtractionReference(dependency).requireNonNull(MessageSupplier
                    .format("Could not compute dependency %s of bind %s", dependency, bind));
        return bind.process(self().into(Polyfill::uncheckedCast), fromBase);
    }

    @NotNull
    @Override
    @SuppressWarnings("unchecked")
    public final Set<Entry<String, Object>> entrySet() {
        return streamRefs().collect(Collectors.toSet());
    }

    protected final class OutputReference<T, R> extends KeyedReference<VarBind<? super S, T, ?, R>, R> {
        private final KeyedReference<String, ReferenceIndex> inputReference;

        public OutputReference(VarBind<? super S, T, ?, R> bind, KeyedReference<String, ReferenceIndex> inputReference) {
            super(bind, inputReference.map(v -> Polyfill.uncheckedCast(computeValueFor(bind, v))));

            this.inputReference = inputReference;
        }
    }
}
