package org.comroid.uniform;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class StructureTypeMismatchException extends IllegalArgumentException {
    public StructureTypeMismatchException(DataStructureType<?, ?, ?> errorType) {
        this(errorType, null);
    }

    public StructureTypeMismatchException(
            DataStructureType<?, ?, ?> errorType, @Nullable DataStructureType<?, ?, ?> expected
    ) {
        super(String.format(
                "Illegal Structure type %s; %s expected",
                errorType.typ,
                expected == null ? Arrays.toString(new DataStructureType.Primitive[]{
                        DataStructureType.Primitive.OBJECT, DataStructureType.Primitive.ARRAY
                }) : expected.typ
        ));
    }
}
