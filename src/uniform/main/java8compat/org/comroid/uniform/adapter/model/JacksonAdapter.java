package org.comroid.uniform.adapter.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.comroid.api.Polyfill;
import org.comroid.common.exception.AssertionException;
import org.comroid.uniform.adapter.AbstractSerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.impl.UniArrayNodeImpl;
import org.comroid.uniform.node.impl.UniObjectNodeImpl;
import org.comroid.uniform.node.impl.UniValueNodeImpl;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.comroid.util.StandardValueType.*;

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
                return createArrayNode((ArrayNode) node);
            if (node.isObject())
                return createObjectNode((ObjectNode) node);
            if (node.isValueNode())
                return new UniValueNodeImpl("unknown", this, null, createValueAdapter(node, any -> false));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Invalid %s data: \n%s", getMimeType(), data), e);
        }

        throw new AssertionException();
    }

    @Override
    public UniObjectNode createObjectNode(ObjectNode node) {
        return new UniObjectNodeImpl(this, null, objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {
        }));
    }

    @Override
    public UniArrayNode createArrayNode(ArrayNode node) {
        return new UniArrayNodeImpl(this, null, objectMapper.convertValue(node, new TypeReference<List<Object>>() {
        }));
    }

    @Override
    public ValueAdapter<Object, Object> createValueAdapter(Object nodeBase, final Predicate<Object> setter) {
        return Polyfill.uncheckedCast(new ValueAdapter<ValueNode, Object>((ValueNode) nodeBase) {
            @Override
            public Object asActualType() {
                switch (base.getNodeType()) {
                    case BINARY:
                    case STRING:
                        return base.asText();
                    case BOOLEAN:
                        return base.asBoolean();
                    case NUMBER:
                        double v = base.asDouble(), abs = Math.abs(v);
                        if (v > Integer.MAX_VALUE && v != abs)
                            return DOUBLE;
                        else if (v == abs)
                            return LONG;
                        else return INTEGER;
                    case POJO:
                    case OBJECT:
                        try {
                            return objectMapper.treeToValue(base, Map.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    case ARRAY:
                        try {
                            return objectMapper.treeToValue(base, List.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    case NULL:
                    case MISSING:
                        return null;
                    default:
                        throw new IllegalStateException("Unexpected value: " + base.getNodeType());
                }
            }

            @Override
            protected boolean doSet(Object newValue) {
                return setter.test(newValue);
            }
        });
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
