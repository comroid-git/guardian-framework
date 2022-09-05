package org.comroid.varbind.bind.builder;

import org.comroid.api.ValueType;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class BuilderStep4$Properties<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        extends VarBindBuilderComponent<SELF, EXTR, REMAP, FINAL> {
    private final ValueType<EXTR> valueType;
    private final VarBind.ExtractionMethod extractionMethod;
    private final BiFunction<? super SELF, ? super EXTR, ? extends REMAP> resolver;
    private final Function<RefContainer<?, REMAP>, FINAL> finisher;
    private final Set<VarBind<? extends SELF, ?, ?, ?>> dependencies = new HashSet<>();
    private boolean required = false;
    private boolean ignoreInDB = false;
    private boolean identifier = false;
    private Function<? super SELF, ? extends FINAL> defaultSupplier = null;

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

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> setIgnoreInDB(boolean ignoreInDB) {
        this.ignoreInDB = ignoreInDB;
        return this;
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> setIdentifier(boolean identifier) {
        this.identifier = identifier;
        return this;
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> setDefaultValue(final Supplier<? extends FINAL> defaultSupplier) {
        return setDefaultValue(nil -> defaultSupplier.get());
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> setDefaultValue(@Nullable Function<? super SELF, ? extends FINAL> defaultSupplier) {
        this.defaultSupplier = defaultSupplier;
        return this;
    }

    protected BuilderStep4$Properties(
            GroupBind<SELF> group,
            String fieldName,
            ValueType<EXTR> valueType,
            VarBind.ExtractionMethod extractionMethod,
            BiFunction<? super SELF, ? super EXTR, ? extends REMAP> resolver,
            Function<RefContainer<?, REMAP>, FINAL> finisher
    ) {
        super(group, fieldName);

        this.valueType = valueType;
        this.extractionMethod = extractionMethod;
        this.resolver = resolver;
        this.finisher = finisher;
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> removeDefaultValue() {
        return setDefaultValue((Function<SELF, FINAL>) null);
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> setRequired() {
        return setRequired(true);
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> setOptional() {
        return setRequired(false);
    }

    public BuilderStep4$Properties<SELF, EXTR, REMAP, FINAL> addDependency(VarBind<? extends SELF, ?, ?, ?> varBind) {
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
                = new Binding<>(group, fieldName, required, ignoreInDB, identifier, valueType, extractionMethod, resolver, finisher, defaultSupplier, dependencies);
        group.addChild(binding);
        return binding;
    }
}
