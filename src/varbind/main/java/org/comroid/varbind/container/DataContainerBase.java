package org.comroid.varbind.container;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.adapter.BiStageAdapter;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.ref.*;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class DataContainerBase<S extends DataContainer<? super S>>
        extends ReferenceAtlas.ForMap<String, ReferenceList, VarBind, Object>
        implements DataContainer<S> {
    private final ReferenceStageAdapter<String, VarBind, ReferenceList, Object, KeyedReference<String, ReferenceList>, KeyedReference<VarBind, Object>> adapter;
    private final ContextualProvider context;
    private final GroupBind<S> group;
    private final Set<VarBind<? extends S, Object, ?, Object>> initialValues;
    private final ParameterizedReference<UniObjectNode, UniObjectNode> parser;

    @Override
    public final GroupBind<S> getRootBind() {
        return group;
    }

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    @Override
    public ReferenceStageAdapter<String, VarBind, ReferenceList, Object, KeyedReference<String, ReferenceList>, KeyedReference<VarBind, Object>> getAdvancer() {
        return adapter;
    }

    protected DataContainerBase(
            ContextualProvider context,
            final GroupBind<S> group,
            UniObjectNode initialData
    ) {
        super(
                new ReferenceMap<>(),
                null,
                VarBind::getFieldName
        );
        this.adapter = new BiStageAdapter<String, ReferenceList, VarBind, Object>(
                false,
                group::findChildByName,
                (fname, referenceList) -> {
                    VarBind<? super S, ?, ?, ?> bind = group.findChildByName(fname);
                    if (bind == null)
                        return null;
                    //noinspection unchecked
                    return bind.process(Polyfill.uncheckedCast(this), referenceList);
                }
        ) {
            @Override
            public KeyedReference<VarBind, Object> advance(KeyedReference<String, ReferenceList> reference) {
                return new KeyedReference.Support.Mapped<>(reference, this::advanceKey, this::advanceValue);
            }
        };
        this.context = context;
        this.group = group;
        this.initialValues = updateFrom(initialData);
        this.parser = null;
    }

    @Override
    public final Set<VarBind<? extends S, Object, ?, Object>> updateFrom(UniObjectNode node) {
        final Set<VarBind<? extends S, Object, ?, Object>> initialized = new HashSet<>();
        node.forEach((key, value) -> {
            KeyedReference<String, ReferenceList<Object>> eRef = getExtractionReference(key);
            VarBind<? extends S, ?, ?, Object> bind = getBindByName(key);

            eRef.compute(refs -> {
                if (refs == null)
                    refs = new ReferenceList<>();
                else refs.clear();
                refs.add(value);
                return refs;
            });
            KeyedReference<VarBind, Object> cRef = getComputedReference(bind);
            Object prev = cRef.get();
            if (cRef.get() != prev)
                initialized.add(Polyfill.uncheckedCast(bind));
        });
        return Collections.unmodifiableSet(initialized);
    }

    @Override
    public final Set<VarBind<? extends S, Object, ?, Object>> initiallySet() {
        return initialValues;
    }

    @Override
    public final UniObjectNode toObjectNode(final UniObjectNode node) {
        return parser.apply(node);
    }

    @Override
    public final <E> KeyedReference<String, ReferenceList<Object>> getExtractionReference(String name) {
        return Polyfill.uncheckedCast(getInputReference(name, true));
    }

    @Override
    public final <T> KeyedReference<VarBind, T> getComputedReference(VarBind<?, ?, ?, T> bind) {
        return Polyfill.uncheckedCast(getReference(bind, true));
    }

    @Override
    public final VarBind<? extends S, ?, ?, Object> getBindByName(String name) {
        //noinspection unchecked
        return getAdvancer().advanceKey(name);
    }

    @Override
    protected final KeyedReference<VarBind, Object> createEmptyRef(VarBind bind) {
        //noinspection unchecked
        KeyedReference<String, ReferenceList> base = getExtractionReference(bind);
        if (base == null)
            throw new NullPointerException("Could not create base reference");
        return advanceReference(base);
    }

    @Override
    protected final KeyedReference<VarBind, Object> advanceReference(KeyedReference<String, ReferenceList> inputRef) {
        class OutputReference extends KeyedReference.Support.Base<VarBind, Object> {
            private OutputReference(final VarBind bind) {
                //noinspection unchecked
                super(bind, inputRef.map(refs -> bind.process((DataContainer) self(), refs)));
            }
        }

        String key = inputRef.getKey();
        VarBind bind = getBindByName(key);
        return new OutputReference(bind);
    }

    private <T, R> R computeValueFor(VarBind<? super S, T, ?, R> bind, ReferenceList<T> fromBase) {
        for (VarBind<?, ?, ?, ?> dependency : bind.getDependencies())
            getExtractionReference(dependency).requireNonNull(MessageSupplier
                    .format("Could not compute dependency %s of bind %s", dependency, bind));
        return bind.process(self().into(Polyfill::uncheckedCast), fromBase);
    }

    @Nullable
    @Override
    public final Object put(VarBind key, Object value) {
        return put(key.getFieldName(), value);
    }

    @NotNull
    @Override
    public final Set<Entry<VarBind, Object>> entrySet() {
        return Collections.unmodifiableSet(streamRefs().collect(Collectors.toSet()));
    }
}
