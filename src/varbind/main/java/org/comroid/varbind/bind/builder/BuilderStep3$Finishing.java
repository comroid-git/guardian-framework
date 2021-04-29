package org.comroid.varbind.bind.builder;

import org.comroid.api.Builder;
import org.comroid.api.Polyfill;
import org.comroid.api.ValueType;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.span.Span;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BuilderStep3$Finishing<SELF extends DataContainer<? super SELF>, EXTR, REMAP>
        extends VarBindBuilderComponent<SELF, EXTR, REMAP, Void>
        implements Builder<VarBind<SELF, EXTR, REMAP, REMAP>> {
    private final ValueType<EXTR> valueType;
    private final VarBind.ExtractionMethod extractionMethod;
    private final BiFunction<? super SELF, ? super EXTR, ? extends REMAP> resolver;

    BuilderStep3$Finishing(
            GroupBind<SELF> group,
            String fieldName,
            ValueType<EXTR> valueType,
            VarBind.ExtractionMethod extractionMethod,
            BiFunction<? super SELF,? super EXTR,? extends REMAP> resolver
    ) {
        super(group, fieldName);

        this.valueType = valueType;
        this.extractionMethod = extractionMethod;
        this.resolver = resolver;
    }

    public <C extends Collection<R>, R> BuilderStep4$Properties<SELF, EXTR, REMAP, C> reformatRefs(
            Function<RefContainer<?, REMAP>, C> spanResolver) {
        return new BuilderStep4$Properties<>(
                group,
                fieldName,
                valueType,
                extractionMethod,
                resolver,
                spanResolver
        );
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, REMAP> onceEach() {
        return new BuilderStep4$Properties<>(
                group,
                fieldName,
                valueType,
                extractionMethod,
                resolver,
                remapRefContainer -> remapRefContainer.findAny().get()
        );
    }

    @Deprecated
    public BuilderStep4$Properties<SELF, EXTR, REMAP, Span<REMAP>> intoSpan() {
        return reformatRefs(refs -> new Span<>(refs, Span.DEFAULT_MODIFY_POLICY));
    }

    public <C extends Collection<R>, R> BuilderStep4$Properties<SELF, EXTR, REMAP, C> intoCollection(
            Supplier<C> collectionSupplier) {
        return reformatRefs(refs -> {
            C col = collectionSupplier.get();
            refs.streamValues().map(Polyfill::<R>uncheckedCast).forEach(col::add);
            return Polyfill.uncheckedCast(col);
        });
    }

    @Override
    public VarBind<SELF, EXTR, REMAP, REMAP> build() {
        return onceEach().build();
    }
}
