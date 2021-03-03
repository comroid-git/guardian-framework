package org.comroid.mutatio.pipe;

import org.comroid.mutatio.ref.ReferenceIndex;

import java.util.Collection;

import static org.jetbrains.annotations.ApiStatus.OverrideOnly;

@Deprecated
public interface Pipeable<T> {
    @Deprecated
    default ReferenceIndex<? extends T> pipe() {
        throw new AbstractMethodError("deprecated");
    }

    interface From<T> extends Pipeable<T> {
        @Override
        default ReferenceIndex<? extends T> pipe() {
            return (ReferenceIndex<? extends T>) ReferenceIndex.of(fetchPipeContent());
        }

        @OverrideOnly
        Collection<? extends T> fetchPipeContent();
    }
}
