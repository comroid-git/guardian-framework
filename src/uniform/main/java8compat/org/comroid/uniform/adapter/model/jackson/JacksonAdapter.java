package org.comroid.uniform.adapter.model.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.comroid.common.exception.AssertionException;
import org.comroid.uniform.adapter.AbstractSerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.impl.UniArrayNodeImpl;
import org.comroid.uniform.node.impl.UniObjectNodeImpl;
import org.comroid.uniform.node.impl.UniValueNodeImpl;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class JacksonAdapter extends AbstractSerializationAdapter<JsonNode, ObjectNode, ArrayNode> {
    private final ObjectMapper objectMapper;

    public JacksonAdapter(String mimeType, ObjectMapper objectMapper) {
        super(mimeType, ObjectNode.class, JsonNodeFactory.instance::objectNode, ArrayNode.class, JsonNodeFactory.instance::arrayNode);

        this.objectMapper = objectMapper;
    }

    @Override
    public DataStructureType<JsonNode, ? extends JsonNode, ? extends UniNode> typeOfData(String data) {
        try {
            final JsonNode node = objectMapper.readTree(data);

            if (node.isArray())
                return getArrayType();
            if (node.isObject())
                return getObjectType();
            if (node.isValueNode())
                return null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Invalid %s data: \n%s", getMimeType(), data), e);
        }

        throw new AssertionException();
    }

    @Override
    public UniNode parse(@Nullable String data) {
        try {
            final JsonNode node = objectMapper.readTree(data);

            if (node.isArray())
                return createUniArrayNode((ArrayNode) node);
            if (node.isObject())
                return createUniObjectNode((ObjectNode) node);
            if (node.isValueNode()) {
                return new UniValueNodeImpl("unknown", this, new ValueNodeAdapter((ValueNode) node));
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Invalid %s data: \n%s", getMimeType(), data), e);
        }

        throw new AssertionException();
    }

    @Override
    public UniObjectNode createUniObjectNode(ObjectNode node) {
        return new UniObjectNodeImpl(this, objectMapper.convertValue(node, new TypeReference<Map<String, Object>>(){}));
    }

    @Override
    public UniArrayNode createUniArrayNode(ArrayNode node) {
        return new UniArrayNodeImpl(this, objectMapper.convertValue(node, new TypeReference<List<Object>>() {}));
    }

    public final JsonNode wrapAsNode(Object element) {
        if (element instanceof JsonNode)
            return (JsonNode) element;
        if (element instanceof Map) {
            final ObjectNode obj = JsonNodeFactory.instance.objectNode();
            ((Map<?, ?>) element).forEach((k, v) -> obj.set(String.valueOf(k), wrapAsNode(v)));
            return obj;
        }
        if (element instanceof Collection) {
            final ArrayNode arr = JsonNodeFactory.instance.arrayNode();
            ((Collection<?>) element).forEach(e -> arr.add(wrapAsNode(e)));
            return arr;
        }
        if (element instanceof String)
            return JsonNodeFactory.instance.textNode((String) element);
        return wrapAsNode(String.valueOf(element)); //todo Improve
    }
}
