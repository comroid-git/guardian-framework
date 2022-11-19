package org.comroid.uniform;

import org.comroid.annotations.Upgrade;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.api.ValueType;
import org.comroid.api.io.FileHandle;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.ApiStatus.NonExtendable;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface SerializationAdapter<BAS, OBJ extends BAS, ARR extends BAS> extends ContextualProvider.This<Object>, Serializer<UniNode> {
    String getMimeType();

    DataStructureType.Obj<BAS, OBJ> getObjectType();

    DataStructureType.Arr<BAS, ARR> getArrayType();

    DataStructureType<Object, Object, UniValueNode> getValueType();

    @Upgrade
    static SerializationAdapter upgrade(ContextualProvider context) {
        return context.getFromContext(SerializationAdapter.class).orElseThrow(() ->
                new NoSuchElementException("Could not find any SerializationAdapter in Context"));
    }

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
            return createObjectNode(getObjectType().cast(it));
        if (getArrayType().test(it))
            return createArrayNode(getArrayType().cast(it));
        ValueType<Object> typeOf = StandardValueType.typeOf(it);
        if (typeOf != null)
            return UniValueNode.create(this, typeOf, it);
        throw new IllegalArgumentException(String.format("Unrecognized node type: %s", it.getClass().getName()));
    }

    DataStructureType<BAS, ? extends BAS, ? extends UniNode> typeOfData(String data);

    @Override
    UniNode parse(@Nullable String data) throws IllegalArgumentException;

    @NonExtendable
    default UniObjectNode createObjectNode() {
        return createObjectNode(getObjectType().get());
    }

    default UniObjectNode createObjectNode(Consumer<UniObjectNode> configurator) {
        final UniObjectNode obj = createObjectNode();
        configurator.accept(obj);
        return obj;
    }

    UniObjectNode createObjectNode(OBJ node);

    @NonExtendable
    default UniArrayNode createArrayNode() {
        return createArrayNode(getArrayType().get());
    }

    default UniArrayNode createArrayNode(Consumer<UniArrayNode> configurator) {
        final UniArrayNode arr = createArrayNode();
        configurator.accept(arr);
        return arr;
    }

    UniArrayNode createArrayNode(ARR node);

    ValueAdapter<Object, Object> createValueAdapter(Object nodeBase, final Predicate<Object> setter);
}
