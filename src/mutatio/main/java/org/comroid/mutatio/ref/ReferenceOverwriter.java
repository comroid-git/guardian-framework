package org.comroid.mutatio.ref;

public interface ReferenceOverwriter<I, O, RefI extends Reference<I>, RefO extends Reference<O>> extends Reference.Advancer<I, O> {
    RefO advance(RefI reference);
}
