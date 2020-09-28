package org.comroid.uniform.adapter.json.fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import org.comroid.annotations.Instance;
import org.comroid.uniform.DataStructureType;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public final class FastJSONLib extends SerializationAdapter<JSON, JSONObject, JSONArray> {
    public static @Instance
    final FastJSONLib fastJsonLib = new FastJSONLib();

    private FastJSONLib() {
        super("application/json", JSONObject.class, JSONArray.class);
    }

    @Override
    public DataStructureType<SerializationAdapter<JSON, JSONObject, JSONArray>, JSON, ? extends JSON> typeOfData(String data) {
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
                    return objectType;
                case Array:
                    return arrayType;
            }
        }

        return null;
    }

    @Override
    public UniNode parse(@Nullable String data) {
        final DataStructureType<SerializationAdapter<JSON, JSONObject, JSONArray>, JSON, ? extends JSON> type = typeOfData(data);

        if (type == null)
            throw new IllegalArgumentException("String is not valid JSON: " + data);

        switch (type.typ) {
            case OBJECT:
                return createUniObjectNode(JSONObject.parseObject(data));
            case ARRAY:
                return createUniArrayNode(JSONArray.parseArray(data));
        }

        throw new IllegalArgumentException("Cannot parse JSON Value");
    }

    @Override
    public UniObjectNode createUniObjectNode(JSONObject node) {
        return new UniObjectNode(this, objectAdapter(node));
    }

    @Override
    public UniArrayNode createUniArrayNode(JSONArray node) {
        return new UniArrayNode(this, arrayAdapter(node));
    }

    private UniObjectNode.Adapter objectAdapter(JSONObject node) {
        class Local extends UniObjectNode.Adapter<JSONObject> {
            protected Local(@NotNull JSONObject baseNode) {
                super(baseNode);
            }

            @Override
            public Object put(String key, Object value) {
                return baseNode.put(key, value);
            }

            @Override
            public @NotNull
            Set<Entry<String, Object>> entrySet() {
                return baseNode.entrySet();
            }
        }

        return new Local(node == null ? new JSONObject() : node);
    }

    private UniArrayNode.Adapter arrayAdapter(JSONArray node) {
        class Local extends UniArrayNode.Adapter<JSONArray> {
            private Local(@NotNull JSONArray node) {
                super(node);
            }

            @Override
            public Object get(int index) {
                return baseNode.get(index);
            }

            @Override
            public Object set(int index, Object element) {
                return baseNode.set(index, element);
            }

            @Override
            public void add(int index, Object element) {
                baseNode.add(index, element);
            }

            @Override
            public Object remove(int index) {
                return baseNode.remove(index);
            }

            @Override
            public int size() {
                return baseNode.size();
            }
        }

        return new Local(node == null ? new JSONArray() : node);
    }
}
