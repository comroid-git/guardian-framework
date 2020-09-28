package org.comroid.uniform;

import org.comroid.api.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.api.Provider;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;

import java.util.function.Supplier;

public class DataStructureType<SERI extends SerializationAdapter<BAS, ?, ?>, BAS, TAR extends BAS> implements Supplier<TAR> {
    public final Primitive typ;
    protected final Class<TAR> tarClass;

    protected DataStructureType(Class<TAR> tarClass, Primitive typ) {
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

    public Class<TAR> typeClass() {
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

    public static class Obj<SERI extends SerializationAdapter<BAS, OBJ, ARR>, BAS, OBJ extends BAS, ARR extends BAS>
            extends DataStructureType<SERI, BAS, OBJ> {
        private final Invocable<OBJ> constructor;

        public Obj(Class<OBJ> objClass) {
            super(objClass, Primitive.OBJECT);

            this.constructor = Invocable.ofConstructor(tarClass);
        }

        public Obj(
                Class<OBJ> objClass, Supplier<? extends OBJ> objectSupplier
        ) {
            super(objClass, Primitive.OBJECT);

            //todo fix & improve
            this.constructor = Polyfill.uncheckedCast(Invocable.ofProvider(Provider.of(objectSupplier)));
        }

        @Override
        public OBJ get() {
            return constructor.autoInvoke();
        }
    }

    public static class Arr<SERI extends SerializationAdapter<BAS, OBJ, ARR>, BAS, OBJ extends BAS, ARR extends BAS>
            extends DataStructureType<SERI, BAS, ARR> {
        private final Invocable<ARR> constructor;

        public Arr(
                Class<ARR> arrClass
        ) {
            super(arrClass, Primitive.ARRAY);

            this.constructor = Invocable.ofConstructor(tarClass);
        }

        public Arr(
                Class<ARR> arrClass, Supplier<? extends ARR> arraySupplier
        ) {
            super(arrClass, Primitive.ARRAY);

            //todo fix & improve
            this.constructor = Polyfill.uncheckedCast(Invocable.ofProvider(Provider.of(arraySupplier)));
        }

        @Override
        public ARR get() {
            return constructor.autoInvoke();
        }
    }
}
