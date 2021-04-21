package org.comroid.restless;

import org.comroid.api.Named;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MimeType implements Named {
    private static final Map<String, MimeType> cache = new ConcurrentHashMap<>();
    public static final MimeType JSON = forName("application/json");
    public static final MimeType XML = forName("application/xml");
    public static final MimeType JAVASCRIPT = forName("application/javascript");
    public static final MimeType HTML = forName("text/html");
    private final String name;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getAlternateFormattedName() {
        return getSuffix();
    }

    public String getPrefix() {
        return name.split("/")[0];
    }

    public String getSuffix() {
        return name.split("/")[1];
    }

    private MimeType(String name) {
        this.name = name;
    }

    public static MimeType forName(String name) {
        return cache.computeIfAbsent(name, MimeType::new);
    }

    @Override
    public String toString() {
        return name;
    }
}
