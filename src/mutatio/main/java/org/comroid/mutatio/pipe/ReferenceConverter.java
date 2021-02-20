package org.comroid.mutatio.pipe;

import org.comroid.mutatio.ref.Reference;

public interface ReferenceConverter<RefI extends Reference<?>, RefO extends Reference<?>> {
    boolean isIdentityValue();

    RefO advance(RefI reference);
}
