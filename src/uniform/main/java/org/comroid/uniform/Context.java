package org.comroid.uniform;

import org.comroid.annotations.Upgrade;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public interface Context extends ContextualProvider {
    default Stream<CharSequence> getSupportedMimeTypes() {
        return streamContextMembers(true)
                .filter(Serializer.class::isInstance)
                .map(Serializer.class::cast)
                .map(Serializer::getMimeType)
                .distinct();
    }

    @Upgrade
    static Context upgrade(final ContextualProvider underlying) {
        return new Context() {
            @Override
            public Stream<Object> streamContextMembers(boolean includeChildren) {
                return underlying.streamContextMembers(includeChildren);
            }

            @Override
            public String getName() {
                return underlying.getName();
            }
        };
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    default <T> SerializationAdapter findSerializer(@Nullable CharSequence mimetype) {
        return (SerializationAdapter) ContextualProvider.super.findSerializer(mimetype);
    }

    default UniObjectNode createObjectNode() {
        return createObjectNode(null);
    }

    default UniObjectNode createObjectNode(@Nullable CharSequence mimeType) {
        return createUniNode(mimeType, NodeType.OBJECT).asObjectNode();
    }

    default UniArrayNode createArrayNode() {
        return createArrayNode(null);
    }

    default UniArrayNode createArrayNode(@Nullable CharSequence mimeType) {
        return createUniNode(mimeType, NodeType.ARRAY).asArrayNode();
    }

    default UniNode createUniNode(@Nullable CharSequence mimeType, NodeType type) {
        SerializationAdapter serializer = findSerializer(mimeType);

        switch (type) {
            case OBJECT:
                return serializer.createObjectNode();
            case ARRAY:
                return serializer.createArrayNode();
            default:
            case VALUE:
                throw new IllegalArgumentException("Cannot explicitly create UniValueNode");
        }
    }

    default UniNode parse(CharSequence mimeType, String parse) {
        return findSerializer(mimeType).parse(parse);
    }
}
