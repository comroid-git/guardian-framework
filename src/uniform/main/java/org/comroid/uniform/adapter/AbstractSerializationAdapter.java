package org.comroid.uniform.adapter;

import org.comroid.api.ContextualProvider;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.node.UniValueNode;

import java.util.function.Supplier;

public abstract class AbstractSerializationAdapter<BAS, OBJ extends BAS, ARR extends BAS> extends ContextualProvider.Base implements SerializationAdapter<BAS, OBJ, ARR> {
    private final String mimeType;
    private final DataStructureType.Obj<BAS, OBJ> objectType;
    private final DataStructureType.Arr<BAS, ARR> arrayType;
    private final DataStructureType<Object, Object, UniValueNode> valueType;

    @Override
    public final String getMimeType() {
        return mimeType;
    }

    @Override
    public DataStructureType.Obj<BAS, OBJ> getObjectType() {
        return objectType;
    }

    @Override
    public DataStructureType.Arr<BAS, ARR> getArrayType() {
        return arrayType;
    }

    @Override
    public DataStructureType<Object, Object, UniValueNode> getValueType() {
        return valueType;
    }

    protected AbstractSerializationAdapter(
            String mimeType,
            Class<? extends OBJ> objType,
            Supplier<? extends OBJ> objFactory,
            Class<? extends ARR> arrType,
            Supplier<? extends ARR> arrFactory
    ) {
        super("Serializer(" + mimeType + ")");
        this.mimeType = mimeType;
        this.objectType = new DataStructureType.Obj<>(this, objType, objFactory);
        this.arrayType = new DataStructureType.Arr<>(this, arrType, arrFactory);
        this.valueType = new DataStructureType<Object, Object, UniValueNode>(this, Object.class, null) {
            @Override
            public Object get() {
                return new Object();
            }
        };
    }

    protected AbstractSerializationAdapter(
            String mimeType,
            DataStructureType.Obj<BAS, OBJ> objectType,
            DataStructureType.Arr<BAS, ARR> arrayType
    ) {
        this.mimeType = mimeType;
        this.objectType = objectType;
        this.arrayType = arrayType;
        this.valueType = new DataStructureType<Object, Object, UniValueNode>(this, Object.class, null) {
            @Override
            public Object get() {
                return new Object();
            }
        };
    }
}
