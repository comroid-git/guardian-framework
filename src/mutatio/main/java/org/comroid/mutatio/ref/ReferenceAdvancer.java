package org.comroid.mutatio.ref;

public interface ReferenceAdvancer<I, O, RefI extends Reference<I>, RefO extends Reference<O>> {
    RefO advance(RefI reference);
}
