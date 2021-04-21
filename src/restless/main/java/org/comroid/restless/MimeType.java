package org.comroid.restless;

import org.comroid.api.Named;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MimeType implements Named, CharSequence {
    private static final Map<String, MimeType> cache = new ConcurrentHashMap<>();
    public static final MimeType JSON = forName("application/json");
    public static final MimeType XML = forName("application/xml");
    public static final MimeType JAVASCRIPT = forName("application/javascript");
    public static final MimeType HTML = forName("text/html");
    private final String name;
    private final int separator;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlternateFormattedName() {
        return getType();
    }

    public String getPrefix() {
        return name.substring(0, separator);
    }

    public String getType() {
        return name.substring(separator + 1);
    }

    private MimeType(String name) {
        this.name = name;
        this.separator = name.indexOf('/');
    }

    public static MimeType forName(String name) {
        return cache.computeIfAbsent(name, MimeType::new);
    }

    @Override
    public int length() {
        return name.length();
    }

    @Override
    public char charAt(int index) {
        return name.charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return name.subSequence(start, end);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof String)
            return name.equals(o);
        if (o instanceof MimeType)
            return name.equals(((MimeType) o).name);
        return false;
    }
}
