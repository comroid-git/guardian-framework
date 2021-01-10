package org.comroid.varbind.bind.builder;

import org.comroid.mutatio.span.Span;
import org.comroid.uniform.ValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BuilderStep4<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        extends VarBindBuilderComponent<SELF,EXTR,REMAP,FINAL> {
    private final ValueType<EXTR> valueType;
    private final VarBind.ExtractionMethod extractionMethod;
    private final BiFunction<? super SELF, ? super EXTR, ? extends REMAP> resolver;
    private final Function<Span<REMAP>, FINAL> finisher;
    private boolean required = false;

    protected BuilderStep4(
            GroupBind<SELF> group,
            String fieldName,
            ValueType<EXTR> valueType,
            VarBind.ExtractionMethod extractionMethod,
            BiFunction<? super SELF,? super EXTR,? extends REMAP> resolver,
            Function<Span<REMAP>, FINAL> finisher
    ) {
        super(group, fieldName);

        this.valueType = valueType;
        this.extractionMethod = extractionMethod;
        this.resolver = resolver;
        this.finisher = finisher;
    }
    @Override
    public GroupBind<SELF> getGroupBind() {
        return group;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    public boolean isRequired() {
        return required;
    }

    public BuilderStep4<SELF, EXTR, REMAP, FINAL> setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public BuilderStep4<SELF, EXTR, REMAP, FINAL> setRequired() {
        return setRequired(true);
    }

    public BuilderStep4<SELF, EXTR, REMAP, FINAL> setOptional() {
        return setRequired(false);
    }

    @Override
    public VarBind<SELF, EXTR, REMAP, FINAL> build() {
        if (valueType == null)
            throw new IllegalArgumentException("ValueType is Missing!");
        if (extractionMethod == null)
            throw new AssertionError("unreachable: ExtractionMethod missing");
        if (resolver == null)
            throw new IllegalArgumentException("No Remapping method defined");
        if (finisher == null)
            throw new IllegalArgumentException("No Finishing method defined");
        if (finisher.apply(new Span<>()) instanceof Collection && extractionMethod != VarBind.ExtractionMethod.ARRAY)
            throw new IllegalArgumentException("Finisher returns Collection but extraction method is not ARRAY");
        Binding<SELF, EXTR, REMAP, FINAL> binding
                = new Binding<>(group, fieldName, required, valueType, extractionMethod, resolver, finisher);
        group.addChild(binding);
        return binding;
    }
}
