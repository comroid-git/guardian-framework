package org.comroid.varbind.bind.impl;

import org.comroid.api.HeldType;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.impl.ValueTypeBase;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Binding<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        implements VarBind<SELF, EXTR, REMAP, FINAL> {
    private final GroupBind<SELF> group;
    private final String fieldName;
    private final boolean required;
    private final HeldType<EXTR> valueType;
    private final ExtractionMethod extractionMethod;
    private final BiFunction<? super SELF, ? super EXTR, ? extends REMAP> remapper;
    private final Function<? super Span<REMAP>, ? extends FINAL> finisher;

    @Override
    public HeldType<EXTR> getHeldType() {
        return valueType;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public GroupBind<SELF> getGroup() {
        return group;
    }

    Binding(
            GroupBind<SELF> group,
            String fieldName,
            boolean required,
            HeldType<EXTR> valueType,
            ExtractionMethod extractionMethod,
            BiFunction<? super SELF, ? super EXTR, ? extends REMAP> remapper,
            Function<? super Span<REMAP>, ? extends FINAL> finisher
    ) {
        this.group = group;
        this.fieldName = fieldName;
        this.required = required;
        this.valueType = valueType;
        this.extractionMethod = extractionMethod;
        this.remapper = remapper;
        this.finisher = finisher;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Binding<?, ?, ?, ?> binding = (Binding<?, ?, ?, ?>) o;
        return group.equals(binding.group) && fieldName.equals(binding.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, fieldName);
    }

    @Override
    public String toString() {
        return getAlternateFormattedName();
    }

    @Override
    public Span<EXTR> extract(UniNode from) {
        final UniNode target = fieldName.isEmpty() ? from : from.get(fieldName);

        switch (extractionMethod) {
            case VALUE:
                assert valueType instanceof ValueTypeBase;
                EXTR value = target.as(((ValueType<EXTR>) valueType));
                return Span.immutable(value);
            case OBJECT:
                assert group.getFromContext().objectValue.equals(valueType);
                UniObjectNode obj = target.asObjectNode();
                EXTR cast = Polyfill.uncheckedCast(obj);
                return Span.immutable(cast);
            case ARRAY:
                if (valueType instanceof ValueTypeBase) {
                    // extract values array
                    return target.asNodeList()
                            .stream()
                            .map(each -> each.as(((ValueType<EXTR>) valueType)))
                            .collect(Span.collector());
                } else if (group.getFromContext().arrayValue.equals(valueType)) {
                    // extract uninode array
                    return target.asNodeList()
                            .stream()
                            .map(UniNode::asObjectNode)
                            // assume EXTR = UniObjectNode !!
                            .map(Polyfill::<EXTR>uncheckedCast)
                            .collect(Span.collector());
                }
        }
        throw new AssertionError("unreachable");
    }

    @Override
    public REMAP remap(SELF context, EXTR data) {
        return remapper.apply(context, data);
    }

    @Override
    public boolean isListing() {
        return extractionMethod == ExtractionMethod.ARRAY;
    }

    @Override
    public FINAL finish(Span<REMAP> parts) {
        return finisher.apply(parts);
    }
}
