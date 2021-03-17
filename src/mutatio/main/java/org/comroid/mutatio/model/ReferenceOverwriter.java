package org.comroid.mutatio.model;

import org.comroid.mutatio.pipe.ReferenceConverter;
import org.comroid.mutatio.ref.Reference;

public interface ReferenceOverwriter<I, O, RefI extends Reference<I>, RefO extends Reference<O>> extends ReferenceConverter<RefI, RefO> {
}
