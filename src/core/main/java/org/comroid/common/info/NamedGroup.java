package org.comroid.common.info;

import org.comroid.api.IntEnum;
import org.comroid.api.Named;
import org.comroid.util.Bitmask;

public interface NamedGroup extends Named, IntEnum {
    @Override
    String getName();

    @Override
    int getValue();

    static NamedGroup of(String name) {
        return of(name, Bitmask.nextFlag(1));
    }

    static NamedGroup of(String name, int value) {
        return new Support.Of(name, value);
    }

    final class Support {
        private static final class Of extends Base implements NamedGroup {
            private final int value;

            @Override
            public int getValue() {
                return value;
            }

            private Of(String name, int value) {
                super(name);

                this.value = value;
            }
        }
    }
}
