package org.comroid.uniform.adapter;

import org.comroid.api.HeldType;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class AbstractSerializationAdapter<BAS, OBJ extends BAS, ARR extends BAS> implements SerializationAdapter<BAS, OBJ, ARR> {
    private final String mimeType;
    private final DataStructureType.Obj<BAS, OBJ> objectType;
    private final ParsingValueType<OBJ, UniObjectNode> objectValue;
    private final DataStructureType.Arr<BAS, ARR> arrayType;
    private final ParsingValueType<ARR, UniArrayNode> arrayValue;

    @Override
    public final String getMimeType() {
        return mimeType;
    }

    @Override
    public DataStructureType.Obj<BAS, OBJ> getObjectType() {
        return objectType;
    }

    @Override
    public ParsingValueType<OBJ, UniObjectNode> getObjectValueType() {
        return objectValue;
    }

    @Override
    public DataStructureType.Arr<BAS, ARR> getArrayType() {
        return arrayType;
    }

    @Override
    public ParsingValueType<ARR, UniArrayNode> getArrayValueType() {
        return arrayValue;
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
        this(mimeType,
                objFactory == null
                        ? new DataStructureType.Obj<>(objType)
                        : new DataStructureType.Obj<>(objType, objFactory),
                arrFactory == null
                        ? new DataStructureType.Arr<>(arrType)
                        : new DataStructureType.Arr<>(arrType, arrFactory));
    }

    protected AbstractSerializationAdapter(
            String mimeType,
            DataStructureType.Obj<BAS, OBJ> objectType,
            DataStructureType.Arr<BAS, ARR> arrayType
    ) {
        this.mimeType = mimeType;
        this.objectType = objectType;
        this.objectValue = new ParsingValueType<>(objectType);
        this.arrayType = arrayType;
        this.arrayValue = new ParsingValueType<>(arrayType);
    }

    @Override
    public String toString() {
        return String.format(
                "%s{object=%s;array=%s}",
                getClass().getSimpleName(),
                objectType.typeClass().getName(),
                arrayType.typeClass().getName()
        );
    }

    private final class ParsingValueType<TGT extends BAS, T extends UniNode> implements ValueType<T> {
        private final DataStructureType<BAS, TGT, T> dst;

        @Override
        public T parse(String data) {
            //noinspection unchecked
            return (T) AbstractSerializationAdapter.this.parse(data);
        }

        @Override
        public String getName() {
            return dst.typ.name();
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof ParsingValueType)
                return ((ParsingValueType<?, ?>) other).getTargetClass().equals(getTargetClass())
                        && ((ParsingValueType<?, ?>) other).dst.typ == dst.typ;
            return false;
        }

        @Override
        public String toString() {
            return String.format("ParsingValueType<%s>", dst.toString());
        }

        public ParsingValueType(DataStructureType<BAS, TGT, T> dst) {
            this.dst = dst;
        }

        @Override
        @Experimental
        public <T1> T1 convert(T value, HeldType<T1> toType) {
            //noinspection unchecked
            final UniNode uni = dst.typ == DataStructureType.Primitive.OBJECT
                    ? createUniObjectNode((OBJ) value)
                    : createUniArrayNode(((ARR) value));

            if (uni.size() == 1)
                //noinspection unchecked
                return (T1) uni.asList().get(0);
            throw new UnsupportedOperationException("Node too large");
        }

        @Override
        public Class<T> getTargetClass() {
            //noinspection unchecked
            return (Class<T>) dst.typeClass();
        }
    }
}
