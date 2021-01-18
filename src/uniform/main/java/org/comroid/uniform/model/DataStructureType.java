package org.comroid.uniform.model;

import org.comroid.api.ContextualProvider;
import org.comroid.api.ValueType;
import org.comroid.api.Named;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

import static org.jetbrains.annotations.ApiStatus.Experimental;

public abstract class DataStructureType<BAS, TAR extends BAS, UNI extends UniNode> implements ValueType<UNI>, Supplier<TAR> {
    protected final Primitive typ;
    protected final Class<? extends TAR> baseClass;
    protected final Class<? extends UniNode> uniClass;
    private final @NotNull SerializationAdapter seriLib;

    @Override
    public final String getName() {
        return typ.name();
    }

    public Primitive getTyp() {
        return typ;
    }

    @Override
    public final Class<UNI> getTargetClass() {
        //noinspection unchecked
        return (Class<UNI>) getBaseClass();
    }

    public final Class<? extends TAR> getBaseClass() {
        return baseClass;
    }

    protected DataStructureType(ContextualProvider seriLib, Class<? extends TAR> baseClass, Primitive typ) {
        this.seriLib = seriLib.requireFromContext(SerializationAdapter.class);
        this.baseClass = baseClass;
        this.uniClass = (typ == Primitive.OBJECT ? UniObjectNode.class : UniArrayNode.class);
        this.typ = typ;
    }

    @Override
    public final UNI parse(String data) {
        UniNode parse = seriLib.parse(data);
        //noinspection unchecked
        return (UNI) (typ == Primitive.OBJECT ? parse.asObjectNode() : parse.asArrayNode());
    }

    @Experimental
    public <T1> T1 convert(UNI value, ValueType<T1> toType) {
        if (value.isValueNode())
            return value.as(toType);

        //noinspection unchecked
        final UniNode uni = typ == DataStructureType.Primitive.OBJECT
                ? seriLib.createUniObjectNode(value)
                : seriLib.createUniArrayNode(value);

        if (uni.size() == 1)
            //noinspection unchecked
            return (T1) uni.stream();
        throw new UnsupportedOperationException("Node too large");
    }

    public TAR cast(Object node) throws ClassCastException {
        if (baseClass.isInstance(node)) {
            return baseClass.cast(node);
        }

        throw new ClassCastException(String.format(
                "Cannot cast %s to targeted %s type %s",
                node.getClass()
                        .getName(),
                typ.name(),
                baseClass.getName()
        ));
    }

    public enum Primitive implements Named {
        OBJECT,
        ARRAY;

        @Override
        public String getName() {
            return name();
        }
    }

    private static abstract class DstBase<BAS, TAR extends BAS, UNI extends UniNode>
            extends DataStructureType<BAS, TAR, UNI> {
        private final Supplier<? extends TAR> instanceFactory;

        protected DstBase(SerializationAdapter seriLib, Class<? extends TAR> tarClass, Supplier<? extends TAR> instanceFactory, Primitive typ) {
            super(seriLib, tarClass, typ);

            this.instanceFactory = instanceFactory;
        }

        @Override
        public final TAR get() {
            return instanceFactory.get();
        }
    }

    public static class Obj<BAS, OBJ extends BAS> extends DstBase<BAS, OBJ, UniObjectNode> {
        public Obj(SerializationAdapter seriLib, Class<? extends OBJ> tarClass, Supplier<? extends OBJ> instanceFactory) {
            super(seriLib, tarClass, instanceFactory, Primitive.OBJECT);
        }
    }

    public static class Arr<BAS, ARR extends BAS> extends DstBase<BAS, ARR, UniArrayNode> {
        public Arr(SerializationAdapter seriLib, Class<? extends ARR> tarClass, Supplier<? extends ARR> instanceFactory) {
            super(seriLib, tarClass, instanceFactory, Primitive.ARRAY);
        }
    }
}
