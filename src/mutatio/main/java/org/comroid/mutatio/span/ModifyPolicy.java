package org.comroid.mutatio.span;

public interface ModifyPolicy {
    boolean canInitialize(Object var);

    boolean canIterate(Object var);

    boolean canOverwrite(Object old, Object with);

    boolean canRemove(Object var);

    boolean canCleanup(Object var);

    void fail(String message) throws NullPointerException;
}
