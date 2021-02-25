package org.comroid.varbind.container;

import org.comroid.api.ContextualProvider;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.ReferenceAtlas;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.ref.ReferenceMap;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class DataContainerBase<S extends DataContainer<? super S>>
        extends ReferenceAtlas<String, VarBind, ReferenceIndex<Object>, Object
        , KeyedReference<String, ReferenceIndex<Object>>, DataContainerBase.OutputReference<Object, Object>>
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
                comparator,
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
    protected OutputReference<Object, Object> advanceReference(KeyedReference<String, ReferenceIndex> inputRef) {
        String key = inputRef.getKey();
        VarBind bind = keyAdvancer.apply(key);
        return new OutputReference(bind, inputRef);
    }

    private <T, R> R computeValueFor(VarBind<? super S, T, ?, R> bind, ReferenceIndex<?,T> fromBase) {
        for (VarBind<?, ?, ?, ?> dependency : bind.getDependencies())
            getExtractionReference(dependency).requireNonNull(MessageSupplier
                    .format("Could not compute dependency %s of bind %s", dependency, bind));
        return bind.
    }

    protected final class OutputReference<T, R> extends KeyedReference<VarBind<? super S, T, ?, R>, R> {
        private final KeyedReference<String, Object> inputReference;

        public OutputReference(VarBind<? super S, Object, ?, R> bind, KeyedReference<String, ReferenceIndex<?, Object>> inputReference) {
            super(bind, inputReference.map(v -> computeValueFor(bind, v)));
            this.inputReference = inputReference;
        }
    }
}
