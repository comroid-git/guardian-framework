package org.comroid.uniform.adapter;

import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class AbstractSerializationAdapter<BAS, OBJ extends BAS, ARR extends BAS> implements SerializationAdapter<BAS, OBJ, ARR> {
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
            Class<? extends ARR> arrType
    ) {
        this(mimeType, objType, null, arrType, null);
    }

    protected AbstractSerializationAdapter(
            String mimeType,
            Class<? extends OBJ> objType,
            @Nullable Supplier<? extends OBJ> objFactory,
            Class<? extends ARR> arrType,
            @Nullable Supplier<? extends ARR> arrFactory
    ) {
        this.mimeType = mimeType;
        this.objectType = (objFactory == null
                ? new DataStructureType.Obj<>(this, objType)
                : new DataStructureType.Obj<>(this, objType, objFactory));
        this.arrayType = (arrFactory == null
                ? new DataStructureType.Arr<>(this, arrType)
                : new DataStructureType.Arr<>(this, arrType, arrFactory));
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
