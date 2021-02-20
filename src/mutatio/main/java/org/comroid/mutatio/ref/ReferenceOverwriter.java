package org.comroid.mutatio.ref;

import org.comroid.mutatio.pipe.ReferenceConverter;

public interface ReferenceOverwriter<I, O, RefI extends Reference<I>, RefO extends Reference<O>> extends ReferenceConverter<RefI, RefO> {
    RefO advance(RefI reference);
}
