package org.comroid.webkit.frame;

import org.comroid.api.Builder;
import org.comroid.api.StringSerializable;
import org.comroid.webkit.config.WebkitConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.*;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public final class FrameBuilder implements Builder<Document>, StringSerializable {
    public static final String RESOURCE_PREFIX = "org.comroid.webkit/";
    public static final String INTERNAL_RESOURCE_PREFIX = "org.comroid.webkit.internal/";
    private static final Map<String, String> partCache = new ConcurrentHashMap<>();
    public static ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    private final Document frame;

    public FrameBuilder() {
        try {
            InputStream frame = classLoader.getResourceAsStream(RESOURCE_PREFIX + "frame.html");
            this.frame = Jsoup.parse(frame, "UTF-8", "");
        } catch (IOException e) {
            throw new RuntimeException("Could not load frame resource", e);
        }

        // add api script
        frame.head().appendElement("script")
                .attr("type", "application/javascript")
                .attr("src", "webkit/api");

        WebkitConfiguration config = WebkitConfiguration.get();
        // read parts
        config.streamPartNames()
                .forEach(part -> {
                    String partData = findPartData(part);

                    // and import to frame
                    frame.getElementsByTag(part).html(partData);
                });
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

    private String findPartData(String part) {
        return partCache.computeIfAbsent(part, k -> {
            String partData;
            try (
                    InputStream in = WebkitConfiguration.get().getPart(part);
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr)
            ) {
                partData = br.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new RuntimeException("Could not read part data");
            }
            return partData;
        });
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
