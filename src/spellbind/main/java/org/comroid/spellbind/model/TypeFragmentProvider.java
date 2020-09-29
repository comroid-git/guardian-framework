package org.comroid.spellbind.model;

import org.comroid.api.Invocable;

public interface TypeFragmentProvider<T extends TypeFragment<? super T>> {
    Class<T> getInterface();

    Invocable<? extends T> getInstanceSupplier();
}
