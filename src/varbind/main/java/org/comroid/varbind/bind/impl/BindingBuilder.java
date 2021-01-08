package org.comroid.varbind.bind.impl;

import org.comroid.api.HeldType;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.bind.VarBindBuilder;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.comroid.api.Polyfill.uncheckedCast;

public final class BindingBuilder<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        implements VarBindBuilder<SELF, EXTR, REMAP, FINAL> {
    private final GroupBind<SELF> group;
    private final String fieldName;
    private boolean required = false;
    private HeldType<EXTR> valueType;
    private VarBind.ExtractionMethod extractionMethod = VarBind.ExtractionMethod.OBJECT;
    private BiFunction<? super SELF, ? super EXTR, ? extends REMAP> remapper;
    private Function<? super Span<REMAP>, ? extends FINAL> finisher;

    @Override
    public GroupBind<SELF> getGroupBind() {
        return group;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    public BindingBuilder(GroupBind<SELF> group, String fieldName) {
        this.group = group;
        this.fieldName = fieldName;
    }

    @Override
    public VarBindBuilder<SELF, EXTR, REMAP, FINAL> setRequired(boolean required) {
        this.required = required;
        return this;
    }

    @Override
    public <E extends Serializable> VarBindBuilder<SELF, E, REMAP, FINAL> extractAs(ValueType<E> valueType) {
        this.valueType = uncheckedCast(valueType);
        this.extractionMethod = VarBind.ExtractionMethod.VALUE;
        return uncheckedCast(this);
    }

    @Override
    public VarBindBuilder<SELF, UniObjectNode, REMAP, FINAL> extractAsObject() {
        this.valueType = uncheckedCast(group.getFromContext().objectValue);
        this.extractionMethod = VarBind.ExtractionMethod.OBJECT;
        return uncheckedCast(this);
    }

    @Override
    public <R> VarBindBuilder<SELF, R, REMAP, FINAL> extractAsArray(@Nullable ValueType<R> valueType) {
        this.valueType = uncheckedCast(valueType == null ? group.getFromContext().arrayValue : valueType);
        this.extractionMethod = VarBind.ExtractionMethod.ARRAY;
        return uncheckedCast(this);
    }

    @Override
    public VarBindBuilder<SELF, EXTR, EXTR, FINAL> asIdentities() {
        this.remapper = (a,b) -> uncheckedCast(b);
        return uncheckedCast(this);
    }

    @Override
    public <R> VarBindBuilder<SELF, EXTR, R, FINAL> andRemap(
            final Function<? super EXTR, ? extends R> remapper) {
        this.remapper = (a,b) -> uncheckedCast(remapper.apply(b));
        return uncheckedCast(this);
    }

    @Override
    public <R> VarBindBuilder<SELF, EXTR, R, FINAL> andResolve(
            final BiFunction<? super SELF, ? super EXTR, ? extends R> resolver) {
        this.remapper = (a,b) -> uncheckedCast(resolver.apply(a,b));
        return uncheckedCast(this);
    }

    @Override
    public VarBindBuilder<SELF, EXTR, REMAP, REMAP> onceEach() {
        this.finisher = it -> it.into(Polyfill::uncheckedCast);
        return uncheckedCast(this);
    }

    public <C extends Collection<REMAP>> VarBindBuilder<SELF, EXTR, REMAP, C> reformatSpan(
            Function<? super Span<REMAP>, ? extends FINAL> spanResolver) {
        this.finisher = spanResolver;
        return uncheckedCast(this);
    }

    @Override
    public VarBind<SELF, EXTR, REMAP, FINAL> build() throws IllegalArgumentException {
        if (valueType == null)
            throw new IllegalArgumentException("ValueType is Missing!");
        if (extractionMethod == null)
            throw new AssertionError("unreachable: ExtractionMethod missing");
        if (remapper == null)
            throw new IllegalArgumentException("No Remapping method defined");
        if (finisher == null)
            throw new IllegalArgumentException("No Finishing method defined");
        if (finisher.apply(new Span<>()) instanceof Collection && extractionMethod != VarBind.ExtractionMethod.ARRAY)
            throw new IllegalArgumentException("Finisher returns Collection but extraction method is not ARRAY");
        Binding<SELF, EXTR, REMAP, FINAL> binding
                = new Binding<>(group, fieldName, required, valueType, extractionMethod, remapper, finisher);
        group.addChild(binding);
        return binding;
    }
}
