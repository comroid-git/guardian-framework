package org.comroid.varbind.bind;

import org.comroid.api.Named;
import org.comroid.api.ValuePointer;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainer;

public interface VarBind<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        extends Named, ValuePointer<EXTR> {
    String getFieldName();

    @Override
    default String getName() {
        return String.format("VarBind<%s.%s>", getGroup().getName(), getFieldName());
    }

    boolean isRequired();

    default FINAL getFrom(UniObjectNode node) {
        return getFrom(null, node);
    }

    default FINAL getFrom(final SELF dependencyObject, UniObjectNode node) {
        return process(dependencyObject, extract(node));
    }

    default Span<REMAP> remapAll(final SELF context, Span<EXTR> from) {
        return from.pipe()
                .map(each -> remap(context, each))
                .span();
    }

    default FINAL process(final SELF context, Span<EXTR> from) {
        return finish(remapAll(context, from));
    }

    GroupBind<SELF> getGroup();

    Span<EXTR> extract(UniNode data);

    REMAP remap(SELF context, EXTR data);

    boolean isListing();

    FINAL finish(Span<REMAP> parts);

    enum ExtractionMethod {
        VALUE, OBJECT, ARRAY
    }
}
