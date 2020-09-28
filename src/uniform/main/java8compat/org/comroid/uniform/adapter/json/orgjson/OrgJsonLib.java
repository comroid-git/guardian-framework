package org.comroid.uniform.adapter.json.orgjson;

import org.comroid.annotations.Instance;
import org.comroid.uniform.DataStructureType;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

public final class OrgJsonLib extends SerializationAdapter<Object, JSONObject, JSONArray> {
    public static @Instance
    final OrgJsonLib orgJsonLib = new OrgJsonLib();

    protected OrgJsonLib() {
        super("application/json", JSONObject.class, JSONArray.class);
    }

    @Override
    public DataStructureType<SerializationAdapter<Object, JSONObject, JSONArray>, Object, ?> typeOfData(String data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UniNode parse(@Nullable String data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UniObjectNode createUniObjectNode(JSONObject node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UniArrayNode createUniArrayNode(JSONArray node) {
        throw new UnsupportedOperationException();
    }
}
