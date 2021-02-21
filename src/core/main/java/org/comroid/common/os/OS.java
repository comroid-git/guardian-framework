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
    WINDOWS("win"),
    MAC("mac"),
    UNIX("nix", "nux", "aix"),
    SOLARIS("sunos");

    public static final OS current = detect();
    private static final Pattern ArchPattern = Pattern.compile(".*(?<num>\\d{2,3}).*");
    public static final Architecture currentArchitecture = detectArchitecture();

    private final List<String> validators;

    @Override
    public String getName() {
        final String str = name().toLowerCase();
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    OS(String... validators) {
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
        final String arch = System.getProperty("os.arch");
        try {
            Matcher matcher = ArchPattern.matcher(arch);
            String num = Polyfill.regexGroupOrDefault(matcher, "num", null);
            if (num == null)
                throw new IllegalStateException("No numeric identifier found in: " + arch);
            return Architecture.valueOf('x' + num);
        } catch (Exception e) {
            throw new RuntimeException("Invalid Architecture: " + arch, e);
        }
    }

    public enum Architecture implements IntEnum {
        x32, x64, x86;

        @Override
        public @NotNull Integer getValue() {
            return Integer.parseInt(name().substring(1));
        }
    }
}
