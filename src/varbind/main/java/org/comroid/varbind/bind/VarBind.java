package org.comroid.varbind.bind;

import org.comroid.api.Named;
import org.comroid.api.ValuePointer;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainer;

import java.util.Set;

public interface VarBind<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        extends Named, ValuePointer<EXTR> {
    String getFieldName();

    @Override
    default String getName() {
        return getFieldName();
    }

    @Override
    default String getAlternateName() {
        return String.format("VarBind<%s.%s>", getGroup().getName(), getFieldName());
    }

    boolean isRequired();

    Set<VarBind<? extends SELF, ?, ?, ?>> getDependencies();

    GroupBind<SELF> getGroup();

    boolean isListing();

    default FINAL getFrom(UniObjectNode node) {
        return getFrom(null, node);
    }

    default FINAL getFrom(SELF context, UniObjectNode node) {
        return process(context, extract(node));
    }

    default RefContainer<?, REMAP> remapAll(final SELF context, RefContainer<?, EXTR> from) {
        return from.map(each -> remap(context, each));
    }

    default FINAL process(SELF context, RefContainer<?, EXTR> from) {
        return finish(context, remapAll(context, from));
    }

    RefContainer<?, EXTR> extract(UniNode data);

    REMAP remap(SELF context, EXTR data);

    FINAL finish(SELF context, RefContainer<?, REMAP> parts);

    enum ExtractionMethod {
        VALUE, OBJECT, ARRAY
    }
}
