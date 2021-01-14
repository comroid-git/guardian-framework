package org.comroid.varbind.bind.builder;

import org.comroid.api.Builder;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.ValueType;
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

    public <C extends Collection<REMAP>> BuilderStep4$Properties<SELF, EXTR, REMAP, C> reformatSpan(
            Function<Span<REMAP>, C> spanResolver) {
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
                Span::get
        );
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, Span<REMAP>> intoSpan() {
        return reformatSpan(Function.identity());
    }

    public <C extends Collection<REMAP>> BuilderStep4$Properties<SELF, EXTR, REMAP, C> intoCollection(
            Supplier<C> collectionSupplier) {
        return reformatSpan(span -> {
            C col = collectionSupplier.get();
            col.addAll(span);
            return Polyfill.uncheckedCast(col);
        });
    }

    @Override
    public VarBind<SELF, EXTR, REMAP, REMAP> build() {
        return onceEach().build();
    }
}
