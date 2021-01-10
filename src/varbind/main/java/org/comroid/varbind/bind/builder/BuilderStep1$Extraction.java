package org.comroid.varbind.bind.builder;

import org.comroid.api.Polyfill;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public final class BuilderStep1$Extraction<SELF extends DataContainer<? super SELF>>
        extends VarBindBuilderComponent<SELF, Void, Void, Void> {
    public BuilderStep1$Extraction(GroupBind<SELF> group, String fieldName) {
        super(group, fieldName);
    }

    public BuilderStep2$Remapping<SELF, UniObjectNode>
    extractAsArray() {
        return extractAsArray(null);
    }

    public <E extends Serializable> BuilderStep2$Remapping<SELF, E> extractAs(ValueType<E> valueType) {
        return new BuilderStep2$Remapping<>(group, fieldName, valueType, VarBind.ExtractionMethod.VALUE);
    }

    public BuilderStep2$Remapping<SELF, UniObjectNode> extractAsObject() {
        return new BuilderStep2$Remapping<>(
                group,
                fieldName,
                group.getFromContext().getObjectType(),
                VarBind.ExtractionMethod.OBJECT
        );
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
