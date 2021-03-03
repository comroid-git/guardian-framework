package org.comroid.mutatio.pipe;

import org.comroid.mutatio.ref.ReferenceList;

import java.util.Collection;

import static org.jetbrains.annotations.ApiStatus.OverrideOnly;

@Deprecated
public interface Pipeable<T> {
    @Deprecated
    default ReferenceList<? extends T> pipe() {
        throw new AbstractMethodError("deprecated");
    }

    interface From<T> extends Pipeable<T> {
        @Override
        default ReferenceList<? extends T> pipe() {
            return ReferenceList.of(fetchPipeContent());
        }

        @OverrideOnly
        Collection<? extends T> fetchPipeContent();
    }
}
