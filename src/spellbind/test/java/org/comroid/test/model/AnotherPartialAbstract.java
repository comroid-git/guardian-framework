package org.comroid.test.model;

import org.comroid.spellbind.model.TypeFragment;

public interface AnotherPartialAbstract extends TypeFragment<NonAbstract> {
    double getAnother();

    default double sub(double from) {
        return from - getAnother();
    }
}
