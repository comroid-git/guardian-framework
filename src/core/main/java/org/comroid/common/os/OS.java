package org.comroid.common.os;

import org.comroid.api.IntEnum;
import org.comroid.api.Named;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

public enum OS implements Named {
    WINDOWS(".dll", "win"),
    MAC(".so", "mac"),
    UNIX(".so", "nix", "nux", "aix"),
    SOLARIS(".so", "sunos");

    public static final OS current = detect();
    public static final Architecture currentArchitecture = Architecture.detect();

    private final String libExtension;
    private final List<String> validators;

    @Override
    public String getName() {
        final String str = name().toLowerCase();
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    public String getLibraryExtension() {
        return libExtension;
    }

    OS(String libExtension, String... validators) {
        this.libExtension = libExtension;
        this.validators = Collections.unmodifiableList(Arrays.asList(validators));
    }

    private static OS detect() {
        if (current != null)
            return current;

        String osName = System.getProperty("os.name");
        for (OS value : values()) {
            final String osLow = osName.toLowerCase();
            if (value.validators.stream().anyMatch(osLow::contains))
                return value;
        }

        throw new NoSuchElementException("Unknown OS: " + osName);
    }

    public enum Architecture implements IntEnum {
        x16, x32, x64;

        @Override
        public @NotNull Integer getValue() {
            return Integer.parseInt(getValueAsString());
        }

        public @NotNull String getValueAsString() {
            return name().substring(1);
        }

        private static Architecture detect() {
            String arch = System.getProperty("os.arch");
            return Arrays.stream(values())
                    .filter(it -> arch.contains(it.getValueAsString()))
                    .findAny()
                    .orElseThrow(() -> new NoSuchElementException("Unknown architecture: " + arch));
        }
    }
}
