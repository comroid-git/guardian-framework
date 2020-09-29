package org.comroid.test.model;

import org.comroid.spellbind.model.TypeFragment;

public interface PartialAbstract extends TypeFragment<NonAbstract> {
    int getValue();

    default int add(int to) {
        return getValue() + to;
    }
}
