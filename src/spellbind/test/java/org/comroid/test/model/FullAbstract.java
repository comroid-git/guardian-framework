package org.comroid.test.model;

import org.comroid.spellbind.model.TypeFragment;

public interface FullAbstract extends TypeFragment<NonAbstract> {
    boolean invert(boolean val);

    String name();
}
