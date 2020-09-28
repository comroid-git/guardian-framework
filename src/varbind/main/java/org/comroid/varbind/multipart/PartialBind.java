package org.comroid.varbind.multipart;

import org.comroid.api.Polyfill;
import org.comroid.api.Named;
import org.comroid.api.ValuePointer;
import org.comroid.mutatio.span.Span;
import org.comroid.spellbind.annotation.Partial;
import org.comroid.spellbind.model.TypeFragment;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainer;

public final class PartialBind {
    public interface BindFragment extends TypeFragment<BindFragment> {
    }

    @Partial
    public interface Base<MEMBEROF extends DataContainer<? super MEMBEROF>, EXTR, REMAP, FINAL> extends BindFragment, Named, ValuePointer<EXTR> {
        String getFieldName();

        @Override
        default String getName() {
            return getFieldName();
        }

        boolean isRequired();

        default FINAL getFrom(UniObjectNode node) {
            return getFrom(null, node);
        }

        default FINAL getFrom(final MEMBEROF dependencyObject, UniObjectNode node) {
            return process(dependencyObject, as(Extractor.class)
                    .map(Polyfill::<Extractor<EXTR>>uncheckedCast)
                    .orElseThrow(AssertionError::new)
                    .extract(node));
        }

        default Span<REMAP> remapAll(final MEMBEROF dependency, Span<EXTR> from) {
            return from.pipe()
                    .map(each -> as(Remapper.class)
                            .map(Polyfill::<Remapper<Object, EXTR, REMAP>>uncheckedCast)
                            .orElseThrow(AssertionError::new)
                            .remap(dependency, each))
                    .span();
        }

        default FINAL process(final MEMBEROF dependency, Span<EXTR> from) {
            return as(Finisher.class)
                    .map(Polyfill::<Finisher<REMAP, FINAL>>uncheckedCast)
                    .orElseThrow(AssertionError::new)
                    .finish(remapAll(dependency, from));
        }
    }

    @Partial
    public interface Grouped<MEMBEROF extends DataContainer<? super MEMBEROF>> extends BindFragment {
        GroupBind<MEMBEROF> getGroup();
    }

    @Partial
    public interface Extractor<EXTR> extends BindFragment {
        Span<EXTR> extract(UniNode from);
    }

    @Partial
    public interface Remapper<MEMBEROF, EXTR, REMAP> extends BindFragment {
        REMAP remap(MEMBEROF it, EXTR from);
    }

    @Partial
    public interface Finisher<REMAP, FINAL> extends BindFragment {
        boolean isListing();

        FINAL finish(Span<REMAP> parts);
    }
}
