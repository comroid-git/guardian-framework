package org.comroid.mutatio.ref;

@Deprecated
public class OutdateableReference<T> extends Reference.Support.Default<T> {
    public OutdateableReference() {
        this(null);
    }

    public OutdateableReference(T initialValue) {
        super(true, initialValue);
    }
}
