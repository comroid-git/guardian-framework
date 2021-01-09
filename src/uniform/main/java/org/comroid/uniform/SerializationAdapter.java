package org.comroid.uniform;

import org.comroid.api.ContextualProvider;
import org.comroid.common.io.FileHandle;
import org.comroid.uniform.adapter.AbstractSerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.uniform.node.impl.NodeFactory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;

public interface SerializationAdapter<BAS, OBJ extends BAS, ARR extends BAS> extends ContextualProvider.This {
    String getMimeType();

    NodeFactory getNodeFactory();

    DataStructureType.Obj<BAS, OBJ> getObjectType();

    ValueType<UniObjectNode> getObjectValueType();

    DataStructureType.Arr<BAS, ARR> getArrayType();

    ValueType<UniArrayNode> getArrayValueType();

    @NonExtendable
    default UniNode readFile(FileHandle file) {
        return createUniNode(file.getContent());
    }

    @NonExtendable
    default <TAR extends BAS> DataStructureType<BAS, ?, ?> typeOf(
            TAR node
    ) {
        if (getObjectType().typeClass().isInstance(node))
            return getObjectType();
        if (getArrayType().typeClass().isInstance(node))
            return getArrayType();
        throw new IllegalArgumentException("Unknown type: " + node.getClass().getName());
    }

    @NonExtendable
    default UniNode createUniNode(Object it) {
        if (it == null)
            UniValueNode.empty();

        if (it instanceof CharSequence) {
            final String string = it.toString();
            if (((CharSequence) it).length() == 0 && string.isEmpty())
                return null;
            return parse(string);
        }

        if (getObjectValueType().test(it))
            return createUniObjectNode(getObjectType().cast(it));
        if (getArrayValueType().test(it))
            return createUniArrayNode(getArrayType().cast(it));
        ValueType<Object> typeOf = ValueType.typeOf(it);
        if (typeOf != null)
            return
        throw new IllegalArgumentException(String.format("Unrecognized node type: %s", it.getClass().getName()));
    }

    DataStructureType<BAS, ? extends BAS, ? extends UniNode> typeOfData(String data);

    UniNode parse(@Nullable String data);

    @NonExtendable
    default UniObjectNode createUniObjectNode() {
        return createUniObjectNode(getObjectType().get());
    }

    UniObjectNode createUniObjectNode(OBJ node);

    @NonExtendable
    default UniArrayNode createUniArrayNode() {
        return createUniArrayNode(getArrayType().get());
    }

    UniArrayNode createUniArrayNode(ARR node);
}
