package org.comroid.uniform.adapter.json.jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.Objects;

import static org.comroid.api.Polyfill.uncheckedCast;

public class JacksonUtils {
    public static <T> T nodeAs(JsonNode jsonNode, Class<T> targetType) {
        if ((String.class.isAssignableFrom(targetType) || Object.class.equals(targetType)) && jsonNode instanceof TextNode) {
            return uncheckedCast(jsonNode.asText());
        }

        if ((Boolean.class.isAssignableFrom(targetType) || Object.class.equals(targetType)) && jsonNode instanceof BooleanNode) {
            return uncheckedCast(jsonNode.asBoolean());
        }

        if (Number.class.isAssignableFrom(targetType) && jsonNode instanceof NumericNode) {
            if (Objects.class.equals(targetType)) {
                return uncheckedCast(jsonNode.asDouble());
            }

            if (Integer.class.isAssignableFrom(targetType)) {
                return uncheckedCast(jsonNode.asInt());
            }
            if (Long.class.isAssignableFrom(targetType)) {
                return uncheckedCast(jsonNode.asLong());
            }
            if (Double.class.isAssignableFrom(targetType) || Float.class.isAssignableFrom(targetType)) {
                return uncheckedCast(jsonNode.asDouble());
            }
        }

        throw new AssertionError();
    }
}
