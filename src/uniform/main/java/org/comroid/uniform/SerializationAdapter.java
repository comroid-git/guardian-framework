package org.comroid.uniform;

import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.api.ValueType;
import org.comroid.common.io.FileHandle;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface SerializationAdapter<BAS, OBJ extends BAS, ARR extends BAS> extends ContextualProvider.This, Serializer<UniNode> {
    String getMimeType();

    DataStructureType.Obj<BAS, OBJ> getObjectType();

    DataStructureType.Arr<BAS, ARR> getArrayType();

    DataStructureType<Object, Object, UniValueNode> getValueType();

    @NonExtendable
    default UniNode readFile(FileHandle file) {
        return createUniNode(file.getContent());
    }

    @NonExtendable
    default <TAR extends BAS> DataStructureType<? extends BAS, ? extends BAS, ? extends UniNode> typeOf(
            TAR node
    ) {
        if (getObjectType().getBaseClass().isInstance(node))
            return getObjectType();
        if (getArrayType().getBaseClass().isInstance(node))
            return getArrayType();
        throw new IllegalArgumentException("Unknown type: " + node.getClass().getName());
    }

    @NonExtendable
    default UniNode createUniNode(Object it) {
        if (it == null)
            return UniValueNode.NULL;

        if (it instanceof CharSequence) {
            final String string = it.toString();
            if (((CharSequence) it).length() == 0 && string.isEmpty())
                return null;
            return parse(string);
        }

        if (getObjectType().test(it))
            return createUniObjectNode(getObjectType().cast(it));
        if (getArrayType().test(it))
            return createUniArrayNode(getArrayType().cast(it));
        ValueType<Object> typeOf = StandardValueType.typeOf(it);
        if (typeOf != null)
            return UniValueNode.create(this, typeOf, it);
        throw new IllegalArgumentException(String.format("Unrecognized node type: %s", it.getClass().getName()));
    }

    DataStructureType<BAS, ? extends BAS, ? extends UniNode> typeOfData(String data);

    @Override
    UniNode parse(@Nullable String data);

    @NonExtendable
    default UniObjectNode createObjectNode() {
        return createUniObjectNode(getObjectType().get());
    }

    @NonExtendable
    default UniArrayNode createArrayNode() {
        return createUniArrayNode(getArrayType().get());
    }

    UniObjectNode createUniObjectNode(OBJ node);

    UniArrayNode createUniArrayNode(ARR node);

    ValueAdapter<Object, Object> createValueAdapter(Object nodeBase, final Predicate<Object> setter);
}
