package org.comroid.webkit.frame;

import org.comroid.api.Builder;
import org.comroid.api.StringSerializable;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.NoSuchElementException;

public final class FrameBuilder implements Builder<Document>, StringSerializable {
    public static final String RESOURCE_PREFIX = "org.comroid.webkit/";
    public static final String INTERNAL_RESOURCE_PREFIX = "org.comroid.webkit.internal/";
    public static ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    private final Document frame;

    public FrameBuilder() {
        try {
            InputStream frame = classLoader.getResourceAsStream(RESOURCE_PREFIX + "frame.html");
            this.frame = Jsoup.parse(frame, "UTF-8", "");
        } catch (IOException e) {
            throw new RuntimeException("Could not load frame resource", e);
        }
    }

    public static @NotNull InputStream getInternalResource(String name) {
        return getResource(true, name);
    }

    public static @NotNull InputStream getResource(String name) {
        return getResource(false, name);
    }

    public static @NotNull InputStream getResource(boolean internal, String name) {
        String resourceName = internal ? INTERNAL_RESOURCE_PREFIX : RESOURCE_PREFIX + name;
        InputStream resource = classLoader.getResourceAsStream(resourceName);
        if (resource == null)
            if (internal) {
                throw new AssertionError(
                        "Could not find internal resource with name " + name,
                        new NoSuchElementException(String.format("Internal resource %s is missing", name)));
            } else {
                throw new NoSuchElementException(String.format("Could not find resource with name %s (%s)", name, resourceName));
            }
        return resource;
    }

    public Document build() {
        return frame;
    }

    @Override
    public String toSerializedString() {
        return build().toString();
    }

    public StringReader toReader() {
        return new StringReader(toSerializedString());
    }
}
