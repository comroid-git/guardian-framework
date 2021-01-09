package org.comroid.uniform.adapter.json.fastjson;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONValidator;
import org.comroid.annotations.Instance;
import org.comroid.uniform.adapter.AbstractSerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.impl.UniArrayNodeImpl;
import org.comroid.uniform.node.impl.UniObjectNodeImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Set;

public final class FastJSONLib extends AbstractSerializationAdapter<JSON, JSONObject, JSONArray> {
    public static final @Instance
    FastJSONLib fastJsonLib = new FastJSONLib();

    private FastJSONLib() {
        super("application/json", JSONObject.class, JSONArray.class);
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
        DataStructureType<JSON, ? extends JSON, ? extends UniNode> type = typeOfData(data);

        if (type == null)
            throw new IllegalArgumentException("String is not valid JSON: " + data);

        switch (type.getTyp()) {
            case OBJECT:
                return createUniObjectNode(JSONObject.parseObject(data));
            case ARRAY:
                return createUniArrayNode(JSONArray.parseArray(data));
        }

        throw new IllegalArgumentException("Cannot parse JSON Value");
    }

    @Override
    public UniObjectNode createUniObjectNode(JSONObject node) {
        return new UniObjectNodeImpl(this, node);
    }

    @Override
    public UniArrayNode createUniArrayNode(JSONArray node) {
        return new UniArrayNodeImpl(this, node);
    }
}
