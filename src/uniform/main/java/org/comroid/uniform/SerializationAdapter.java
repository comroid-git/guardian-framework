package org.comroid.uniform;

import org.comroid.api.ContextualProvider;
import org.comroid.api.HeldType;
import org.comroid.common.io.FileHandle;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.ApiStatus.Experimental;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

public abstract class SerializationAdapter<BAS, OBJ extends BAS, ARR extends BAS> implements ContextualProvider.This {
    public final String mimeType;
    public final DataStructureType.Arr<SerializationAdapter<BAS, OBJ, ARR>, BAS, OBJ, ARR> arrayType;
    public final DataStructureType.Obj<SerializationAdapter<BAS, OBJ, ARR>, BAS, OBJ, ARR> objectType;
    public final HeldType<OBJ> objectValue;
    public final HeldType<ARR> arrayValue;

    public final String getMimeType() {
        return mimeType;
    }

    protected SerializationAdapter(
            String mimeType, Class<OBJ> objClass, Class<ARR> arrClass
    ) {
        this(mimeType, objClass, null, arrClass, null);
    }

    protected SerializationAdapter(
            String mimeType, Class<OBJ> objClass, Supplier<? extends OBJ> objectSupplier, Class<ARR> arrClass, Supplier<? extends ARR> arraySupplier
    ) {
        this(mimeType, objectSupplier != null
                        ? new DataStructureType.Obj<>(objClass, objectSupplier)
                        : new DataStructureType.Obj<>(objClass),
                arraySupplier != null
                        ? new DataStructureType.Arr<>(arrClass, arraySupplier)
                        : new DataStructureType.Arr<>(arrClass));

        Objects.requireNonNull(objClass, "Object class cannot be null");
    }

    protected SerializationAdapter(
            String mimeType,
            DataStructureType.Obj<SerializationAdapter<BAS, OBJ, ARR>, BAS, OBJ, ARR> objectType,
            DataStructureType.Arr<SerializationAdapter<BAS, OBJ, ARR>, BAS, OBJ, ARR> arrayType
    ) {
        this.mimeType = mimeType;
        this.objectType = objectType;
        this.arrayType = arrayType;
        this.objectValue = new ParsingValueType<>(objectType);
        this.arrayValue = new ParsingValueType<>(arrayType);
    }

    public final UniNode readFile(FileHandle file) {
        return createUniNode(file.getContent());
    }

    @Override
    public String toString() {
        return String.format(
                "%s{object=%s;array=%s}",
                getClass().getSimpleName(),
                objectType.tarClass.getName(),
                arrayType.tarClass.getName()
        );
    }

    public <TAR extends BAS> DataStructureType<SerializationAdapter<BAS, OBJ, ARR>, BAS, TAR> typeOf(
            TAR node
    ) {
        if (objectType.typeClass().isInstance(node)) {
            return (DataStructureType<SerializationAdapter<BAS, OBJ, ARR>, BAS, TAR>) objectType;
        }
        if (arrayType.typeClass().isInstance(node)) {
            return (DataStructureType<SerializationAdapter<BAS, OBJ, ARR>, BAS, TAR>) arrayType;
        }

        throw new IllegalArgumentException("Unknown type: " + node.getClass()
                .getName());
    }

    public final UniNode createUniNode(Object node) {
        if (node == null)
            UniValueNode.empty();

        if (node instanceof String) {
            if (((String) node).isEmpty())
                return null;
            return parse(node == null ? null : node.toString());
        }

        if (objectType.typeClass()
                .isInstance(node)) {
            return createUniObjectNode((OBJ) node);
        }
        if (arrayType.typeClass()
                .isInstance(node)) {
            return createUniArrayNode((ARR) node);
        }

        throw new IllegalArgumentException(String.format(
                "Unknown node type: %s",
                node.getClass()
                        .getName()
        ));
    }

    public abstract DataStructureType<? extends SerializationAdapter<BAS, OBJ, ARR>, BAS, ? extends BAS> typeOfData(String data);

    public abstract UniNode parse(@Nullable String data);

    public UniObjectNode createUniObjectNode() {
        return createUniObjectNode(objectType.get());
    }

    public abstract UniObjectNode createUniObjectNode(OBJ node);

    public UniArrayNode createUniArrayNode() {
        return createUniArrayNode(arrayType.get());
    }

    public abstract UniArrayNode createUniArrayNode(ARR node);

    private final class ParsingValueType<T extends BAS> implements HeldType<T> {
        private final DataStructureType<SerializationAdapter<BAS, OBJ, ARR>, ? super T, ?> dst;

        @Override
        public T parse(String data) {
            return (T) SerializationAdapter.this.parse(data);
        }

        @Override
        public String getName() {
            return dst.typ.name();
        }

        public ParsingValueType(DataStructureType<SerializationAdapter<BAS, OBJ, ARR>, ? super T, ?> dst) {
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
            return (Class<T>) dst.tarClass;
        }
    }

}
