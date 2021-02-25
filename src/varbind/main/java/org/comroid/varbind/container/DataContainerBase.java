package org.comroid.varbind.container;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.ref.*;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class DataContainerBase<S extends DataContainer<? super S>>
        extends ReferenceAtlas<String, VarBind, ReferenceIndex, Object
        , KeyedReference<String, ReferenceIndex>, DataContainerBase.OutputReference>
        implements DataContainer<S> {
    private final ContextualProvider context;
    private final GroupBind<S> group;

    private DataContainerBase(
            ContextualProvider context,
            final GroupBind<S> group,
            @Nullable Comparator<OutputReference> comparator
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
    }

    @Override
    protected OutputReference createEmptyRef(VarBind bind) {
        KeyedReference<String, ReferenceIndex> base = getInputReference(bind.getFieldName(), true);
        if (base == null)
            throw new NullPointerException("Could not create base reference");
        return new OutputReference(bind, base);
    }

    @Override
    protected OutputReference advanceReference(KeyedReference<String, ReferenceIndex> inputRef) {
        String key = inputRef.getKey();
        VarBind bind = keyAdvancer.apply(key);
        return new OutputReference(bind, inputRef);
    }

    private <T, R> R computeValueFor(VarBind<? super S, T, ?, R> bind, ReferenceIndex<?,T> fromBase) {
        for (VarBind<?, ?, ?, ?> dependency : bind.getDependencies())
            getExtractionReference(dependency).requireNonNull(MessageSupplier
                    .format("Could not compute dependency %s of bind %s", dependency, bind));
        return bind.process(self().into(Polyfill::uncheckedCast), fromBase);
    }

    protected final class OutputReference<T, R> extends KeyedReference<VarBind<? super S, T, ?, R>, R> {
        private final KeyedReference<String, ReferenceIndex> inputReference;

        public OutputReference(VarBind<? super S, T, ?, R> bind, KeyedReference<String, ReferenceIndex> inputReference) {
            super(bind, inputReference.map(v -> Polyfill.uncheckedCast(computeValueFor(bind, v))));

            this.inputReference = inputReference;
        }
    }
}
