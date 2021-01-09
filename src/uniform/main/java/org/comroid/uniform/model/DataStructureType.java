package org.comroid.uniform.model;

import org.comroid.api.Invocable;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

import java.util.function.Supplier;

public abstract class DataStructureType<BAS, TAR extends BAS, UNI extends UniNode> implements Supplier<TAR> {
    public final Primitive typ;
    protected final Class<? extends TAR> tarClass;

    protected DataStructureType(Class<? extends TAR> tarClass, Primitive typ) {
        this.tarClass = tarClass;
        this.typ = typ;
    }

    @Override
    public int hashCode() {
        return (31 * tarClass.hashCode()) + typ.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataStructureType<?, ?, ?> that = (DataStructureType<?, ?, ?>) o;

        if (!tarClass.equals(that.tarClass)) {
            return false;
        }
        return typ == that.typ;
    }

    @Override
    public String toString() {
        return String.format("DataStructureType{typ=%s, tarClass=%s}", typ, tarClass);
    }

    public Class<? extends TAR> typeClass() {
        return tarClass;
    }

    public TAR cast(Object node) throws ClassCastException {
        if (tarClass.isInstance(node)) {
            return tarClass.cast(node);
        }

        throw new ClassCastException(String.format(
                "Cannot cast %s to targeted %s type %s",
                node.getClass()
                        .getName(),
                typ.name(),
                tarClass.getName()
        ));
    }

    @Override
    @OverrideOnly
    public TAR get() {
        return null;
    }

    public enum Primitive {
        OBJECT,
        ARRAY
    }

    private static abstract class DstBase<BAS, TAR extends BAS, UNI extends UniNode>
            extends DataStructureType<BAS, TAR, UNI> {
        private final Supplier<? extends TAR> instanceFactory;

        protected DstBase(Class<? extends TAR> tarClass, Supplier<? extends TAR> instanceFactory, Primitive typ) {
            super(tarClass, typ);

            this.instanceFactory = instanceFactory;
        }

        @Override
        public final TAR get() {
            return instanceFactory.get();
        }
    }

    public static class Obj<BAS, OBJ extends BAS> extends DstBase<BAS, OBJ, UniObjectNode> {
        public Obj(final Class<? extends OBJ> tarClass) {
            this(tarClass, () -> Invocable.newInstance(tarClass));
        }

        public Obj(Class<? extends OBJ> tarClass, Supplier<? extends OBJ> instanceFactory) {
            super(tarClass, instanceFactory, Primitive.OBJECT);
        }
    }

    public static class Arr<BAS, ARR extends BAS> extends DstBase<BAS, ARR, UniArrayNode> {
        public Arr(Class<? extends ARR> tarClass) {
            this(tarClass, () -> Invocable.newInstance(tarClass));
        }

        public Arr(Class<? extends ARR> tarClass, Supplier<? extends ARR> instanceFactory) {
            super(tarClass, instanceFactory, Primitive.ARRAY);
        }
    }
}
