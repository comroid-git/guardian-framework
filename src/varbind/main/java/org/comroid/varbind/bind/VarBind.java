package org.comroid.varbind.bind;

import org.comroid.api.Named;
import org.comroid.api.Polyfill;
import org.comroid.api.ValuePointer;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainer;

public interface VarBind<MEMBEROF extends DataContainer<? super MEMBEROF>, EXTR, REMAP, FINAL>
        extends Named, ValuePointer<EXTR> {
    String getFieldName();

    @Override
    default String getName() {
        return String.format("VarBind<%s>", getFieldName());
    }

    boolean isRequired();

    default FINAL getFrom(UniObjectNode node) {
        return getFrom(null, node);
    }

    default FINAL getFrom(final MEMBEROF dependencyObject, UniObjectNode node) {
        return process(dependencyObject, extract(node));
    }

    default Span<REMAP> remapAll(final MEMBEROF context, Span<EXTR> from) {
        return from.pipe()
                .map(each -> remap(context, each))
                .span();
    }

    default FINAL process(final MEMBEROF context, Span<EXTR> from) {
        return finish(remapAll(context, from));
    }

    GroupBind<MEMBEROF> getGroup();

    Span<EXTR> extract(UniNode from);

    REMAP remap(MEMBEROF context, EXTR from);

    boolean isListing();

    FINAL finish(Span<REMAP> parts);
}
