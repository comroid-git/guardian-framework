package org.comroid.uniform.adapter.json.gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.comroid.api.Polyfill;
import org.comroid.api.exception.AssertionException;
import org.comroid.uniform.adapter.AbstractSerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.uniform.node.impl.UniArrayNodeImpl;
import org.comroid.uniform.node.impl.UniObjectNodeImpl;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Predicate;

@ApiStatus.Experimental // untested
public class GsonAdapter extends AbstractSerializationAdapter<JsonElement, JsonObject, JsonArray> {
    protected GsonAdapter() {
        super("application/json", JsonObject.class, JsonObject::new, JsonArray.class, JsonArray::new);
    }

    @Override
    public DataStructureType<JsonElement, ? extends JsonElement, ? extends UniNode> typeOfData(String data) {
        switch (Objects.requireNonNull(Objects.requireNonNull(parse(data)).getNodeType().dst)) {
            case OBJECT:
                return getObjectType();
            case ARRAY:
                return getArrayType();
            default:
                return Polyfill.uncheckedCast(getValueType());
        }
    }

    @Override
    public UniNode parse(@Nullable String data) throws IllegalArgumentException {
        return null;
    }

    @Override
    public UniObjectNode createObjectNode(JsonObject node) {
        return new UniObjectNodeImpl(this, null, new ObjectAdapter(node));
    }

    @Override
    public UniArrayNode createArrayNode(JsonArray node) {
        return new UniArrayNodeImpl(this, null, new ArrayAdapter(node));
    }

    @Override
    public ValueAdapter<Object, Object> createValueAdapter(Object nodeBase, Predicate<Object> setter) {
        return Polyfill.uncheckedCast(new ValueAdapter<>((JsonElement) nodeBase) {
            @Override
            public Object asActualType() {
                if (base == null || base.isJsonNull())
                    return UniValueNode.NULL;
                if (base.isJsonObject())
                    return createObjectNode(base.getAsJsonObject());
                if (base.isJsonArray())
                    return createArrayNode(base.getAsJsonArray());
                if (base.isJsonPrimitive()) {
                    JsonPrimitive prim = (JsonPrimitive) base;
                    if (prim.isBoolean())
                        return prim.getAsBoolean();
                    if (prim.isString())
                        return prim.getAsString();
                    if (prim.isNumber())
                        return prim.getAsNumber();
                }
                throw new AssertionException("unknown type: " + base);
            }

            @Override
            protected boolean doSet(Object newValue) {
                return setter.test(newValue);
            }
        });
    }
}
