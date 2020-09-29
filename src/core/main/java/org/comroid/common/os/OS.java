package org.comroid.common.os;

import org.comroid.api.Named;

import java.util.Arrays;
import java.util.NoSuchElementException;

public enum OS implements Named {
    WINDOWS("win"),
    MAC("mac"),
    UNIX("nix", "nux", "aix"),
    SOLARIS("sunos");

    public static final OS current = detect();

    private final String[] validators;

    @Override
    public String getName() {
        final String str = name().toLowerCase();
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    OS(String... validators) {
        this.validators = validators;
    }

    static OS detect() {
        if (current != null)
            return current;

        for (OS value : values()) {
            if (Arrays.asList(value.validators).contains(System.getProperty("os.name").toLowerCase()))
                return value;
        }

        throw new NoSuchElementException("Unknown OS");
    }
}
