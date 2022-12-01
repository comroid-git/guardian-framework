package org.comroid.uniform.adapter.json.fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.comroid.annotations.Instance;
import org.comroid.uniform.adapter.AbstractSerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.uniform.node.impl.UniArrayNodeImpl;
import org.comroid.uniform.node.impl.UniObjectNodeImpl;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Predicate;

public final class FastJSONLib extends AbstractSerializationAdapter<JSON, JSONObject, JSONArray> {
    public static final @Instance
    FastJSONLib fastJsonLib = new FastJSONLib();

    public FastJSONLib() {
        super("application/json", JSONObject.class, JSONObject::new, JSONArray.class, JSONArray::new);
    }

    @Override
    public DataStructureType<JSON, ? extends JSON, ? extends UniNode> typeOfData(String data) {
        final JSONValidator validator = JSONValidator.from(data);

        if (validator.validate()) {
            final JSONValidator.Type type = validator.getType();

            try {
                validator.close();
            } catch (IOException e) {
                throw new RuntimeException("Could not close validator", e);
            }

            switch (type) {
                case Object:
                    return getObjectType();
                case Array:
                    return getArrayType();
            }
        }

        return null;
    }

    @Override
    public UniNode parse(@Nullable String data) {
        if (data == null || data.isEmpty() || data.equals("null"))
            return UniValueNode.NULL;

        DataStructureType<JSON, ? extends JSON, ? extends UniNode> type = typeOfData(data);

        if (type == null)
            throw new IllegalArgumentException("String is not valid JSON: " + data);

        switch (type.getTyp()) {
            case OBJECT:
                return createObjectNode(JSONObject.parseObject(data));
            case ARRAY:
                return createArrayNode(JSONArray.parseArray(data));
        }

        throw new IllegalArgumentException("Cannot parse JSON Value");
    }

    @Override
    public UniObjectNode createObjectNode(JSONObject node) {
        return new UniObjectNodeImpl(this, null, node) {
            @Override
            public String toString() {
                return JSONObject.toJSONString(baseNode, SerializerFeature.WriteMapNullValue);
            }
        };
    }

    @Override
    public UniArrayNode createArrayNode(JSONArray node) {
        return new UniArrayNodeImpl(this, null, node) {
            @Override
            public String toString() {
                return JSONArray.toJSONString(baseNode, SerializerFeature.WriteMapNullValue);
            }
        };
    }

    @Override
    public ValueAdapter<Object, Object> createValueAdapter(Object nodeBase, final Predicate<Object> setter) {
        return new ValueAdapter<Object, Object>(nodeBase) {
            @Override
            public Object asActualType() {
                return base;
            }

            @Override
            protected boolean doSet(Object newValue) {
                return setter.test(newValue);
            }
        };
    }
}
