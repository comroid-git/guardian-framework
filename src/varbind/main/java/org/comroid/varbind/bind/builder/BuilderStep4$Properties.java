package org.comroid.varbind.bind.builder;

import org.comroid.api.ValueType;
import org.comroid.mutatio.span.Span;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BuilderStep4$Properties<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        extends VarBindBuilderComponent<SELF,EXTR,REMAP,FINAL> {
    private final ValueType<EXTR> valueType;
    private final VarBind.ExtractionMethod extractionMethod;
    private final BiFunction<? super SELF, ? super EXTR, ? extends REMAP> resolver;
    private final Function<Span<REMAP>, FINAL> finisher;
    private final Set<VarBind<? extends SELF,?,?,?>> dependencies = new HashSet<>();
    private boolean required = false;

    protected BuilderStep4$Properties(
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

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> setRequired(boolean required) {
        this.required = required;
        return this;
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> setRequired() {
        return setRequired(true);
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> setOptional() {
        return setRequired(false);
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> addDependency(VarBind<? extends SELF,?,?,?> varBind) {
        dependencies.add(varBind);
        return this;
    }

    public VarBind<SELF, EXTR, REMAP, FINAL> build() {
        if (valueType == null)
            throw new IllegalArgumentException("ValueType is Missing!");
        if (extractionMethod == null)
            throw new AssertionError("unreachable: ExtractionMethod missing");
        if (resolver == null)
            throw new IllegalArgumentException("No Remapping method defined");
        if (finisher == null)
            throw new IllegalArgumentException("No Finishing method defined");
        Binding<SELF, EXTR, REMAP, FINAL> binding
                = new Binding<>(group, fieldName, required, valueType, extractionMethod, resolver, finisher, dependencies);
        group.addChild(binding);
        return binding;
    }
}
