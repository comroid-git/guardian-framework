package org.comroid.varbind.bind.builder;

import org.comroid.api.Polyfill;
import org.comroid.api.ValueType;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Nullable;

public final class BuilderStep1$Extraction<SELF extends DataContainer<? super SELF>>
        extends VarBindBuilderComponent<SELF, Void, Void, Void> {
    public BuilderStep1$Extraction(GroupBind<SELF> group, String fieldName) {
        super(group, fieldName);
    }

    public BuilderStep2$Remapping<SELF, UniObjectNode>
    extractAsArray() {
        return extractAsArray(null);
    }

    public <E> BuilderStep2$Remapping<SELF, E> extractAs(
            @MagicConstant(valuesFromClass = StandardValueType.class) ValueType<E> valueType) {
        return new BuilderStep2$Remapping<>(group, fieldName, valueType, VarBind.ExtractionMethod.VALUE);
    }

    public BuilderStep2$Remapping<SELF, UniObjectNode> extractAsObject() {
        return Polyfill.uncheckedCast(new BuilderStep2$Remapping<SELF, UniObjectNode>(
                group,
                fieldName,
                group.getFromContext().getObjectType(),
                VarBind.ExtractionMethod.OBJECT
        ));
    }

    public <R> BuilderStep2$Remapping<SELF, R> extractAsArray(@Nullable ValueType<R> valueType) {
        return new BuilderStep2$Remapping<>(
                group,
                fieldName,
                Polyfill.uncheckedCast(valueType == null ? group.getFromContext().getArrayType() : valueType),
                VarBind.ExtractionMethod.ARRAY
        );
    }
}
