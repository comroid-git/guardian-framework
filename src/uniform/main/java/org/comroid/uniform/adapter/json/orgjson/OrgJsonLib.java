package org.comroid.uniform.adapter.json.orgjson;

import org.comroid.annotations.Instance;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.adapter.AbstractSerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.function.Predicate;

public final class OrgJsonLib extends AbstractSerializationAdapter<Object, JSONObject, JSONArray> {
    public static @Instance
    final OrgJsonLib orgJsonLib = new OrgJsonLib();

    protected OrgJsonLib() {
        super("application/json", JSONObject.class, JSONObject::new, JSONArray.class, JSONArray::new);
    }

    @Override
    public DataStructureType<Object, ? extends Reference, ? extends UniNode> typeOfData(String data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UniNode parse(@Nullable String data) {
        if (data == null || data.equals("null"))
            return UniValueNode.NULL;
        throw new UnsupportedOperationException();
    }

    @Override
    public UniObjectNode createObjectNode(JSONObject node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UniArrayNode createArrayNode(JSONArray node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ValueAdapter<Object, Object> createValueAdapter(Object nodeBase, final Predicate<Object> setter) {
        throw new UnsupportedOperationException();
    }
}
