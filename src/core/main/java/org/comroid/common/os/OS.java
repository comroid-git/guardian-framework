package org.comroid.common.os;

import org.comroid.api.IntEnum;
import org.comroid.api.Named;
import org.comroid.api.Polyfill;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum OS implements Named {
    WINDOWS(".dll", "win"),
    MAC(".so", "mac"),
    UNIX(".so", "nix", "nux", "aix"),
    SOLARIS(".so", "sunos");

    public static final OS current = detect();
    public static final Architecture currentArchitecture = detectArchitecture();

    private final String libExtension;
    private final List<String> validators;

    @Override
    public String getName() {
        final String str = name().toLowerCase();
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
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

    private static Architecture detectArchitecture() {
        String arch = System.getProperty("os.arch");
        if (arch.contains("16"))
            return Architecture.x16;
        if (arch.contains("32") || arch.contains("86"))
            return Architecture.x32;
        if (arch.contains("64"))
            return Architecture.x64;
        throw new NoSuchElementException("Unknown architecture: " + arch);
    }

    public String getLibraryExtension() {
        return libExtension;
    }

    public enum Architecture implements IntEnum {
        x16, x32, x64;

        @Override
        public @NotNull Integer getValue() {
            return Integer.parseInt(name().substring(1));
        }
    }
}
