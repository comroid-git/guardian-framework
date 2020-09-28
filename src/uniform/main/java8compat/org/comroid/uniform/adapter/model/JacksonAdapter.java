package org.comroid.uniform.adapter.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ValueNode;
import org.comroid.common.exception.AssertionException;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.DataStructureType;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class JacksonAdapter extends SerializationAdapter<JsonNode, ObjectNode, ArrayNode> {
    private final ObjectMapper objectMapper;

    public JacksonAdapter(String mimeType, ObjectMapper objectMapper) {
        super(mimeType, ObjectNode.class, JsonNodeFactory.instance::objectNode, ArrayNode.class, JsonNodeFactory.instance::arrayNode);

        this.objectMapper = objectMapper;
    }

    @Override
    public DataStructureType<SerializationAdapter<JsonNode, ObjectNode, ArrayNode>, JsonNode, ? extends JsonNode> typeOfData(String data) {
        try {
            final JsonNode node = objectMapper.readTree(data);

            if (node.isArray())
                return arrayType;
            if (node.isObject())
                return objectType;
            if (node.isValueNode())
                return null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Invalid %s data: \n%s", mimeType, data), e);
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
            if (node.isValueNode())
                return createValueNode((ValueNode) node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(String.format("Invalid %s data: \n%s", mimeType, data), e);
        }

        throw new AssertionException();
    }

    private UniValueNode<String> createValueNode(ValueNode dataString) {
        return new UniValueNode<>(this, new Reference.Support.Base<String>(false) {
            private final ValueNode base = dataString;

            @Override
            protected String doGet() {
                return base.asText();
            }
        }, ValueType.STRING);
    }

    @Override
    public UniObjectNode createUniObjectNode(ObjectNode node) {
        return new UniObjectNode(this, new UniObjectNode.Adapter<ObjectNode>(node) {
            @Override
            public Object put(String key, Object value) {
                return baseNode.put(key, wrapAsNode(value));
            }

            @Override
            public @NotNull Set<Entry<String, Object>> entrySet() {
                final Iterator<String> keys = baseNode.fieldNames();
                final Set<Entry<String, Object>> yield = new HashSet<>();

                while (keys.hasNext()) {
                    final String key = keys.next();

                    yield.add(new SimpleImmutableEntry<>(key, baseNode.get(key)));
                }

                return yield;
            }
        });
    }

    @Override
    public UniArrayNode createUniArrayNode(ArrayNode node) {
        return new UniArrayNode(this, new UniArrayNode.Adapter(node) {
            @Override
            public int size() {
                return node.size();
            }

            @Override
            public Object get(int index) {
                return node.get(index);
            }

            @Override
            public Object set(int index, Object element) {
                return node.set(index, wrapAsNode(element));
            }

            @Override
            public void add(int index, Object element) {
                if (node.size() >= index || node.size() < index)
                    node.add(wrapAsNode(element));
                else node.set(index, wrapAsNode(element));
            }

            @Override
            public Object remove(int index) {
                return null;
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
