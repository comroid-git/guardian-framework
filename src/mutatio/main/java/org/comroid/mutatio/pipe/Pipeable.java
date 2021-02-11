package org.comroid.mutatio.pipe;

import org.comroid.mutatio.ref.ReferenceIndex;

import java.util.Collection;

import static org.jetbrains.annotations.ApiStatus.OverrideOnly;

@Deprecated
public interface Pipeable<T> {
    ReferenceIndex<? extends T> pipe();

    interface From<T> extends Pipeable<T> {
        @Override
        default ReferenceIndex<? extends T> pipe() {
            return ReferenceIndex.of(fetchPipeContent());
        }

        @OverrideOnly
        Collection<? extends T> fetchPipeContent();
    }
}
