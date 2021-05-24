package org.comroid.webkit.config;

import org.comroid.api.ResourceLoader;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.NoSuchElementException;

public class WebkitResourceLoader {
    public static final String RESOURCE_PREFIX = "org/comroid/webkit/";
    public static final String INTERNAL_RESOURCE_PREFIX = "org/comroid/webkit/internal/";
    public static ResourceLoader LOADER = null;

    public static @NotNull InputStream getInternalResource(String name) {
        return getResource(true, name);
    }

    public static @NotNull InputStream getResource(String name) {
        return getResource(false, name);
    }

    public static @NotNull InputStream getResource(boolean internal, String name) {
        if (LOADER == null)
            throw new NullPointerException("ResourceLoader is not initialized");

        String resourceName = (internal ? INTERNAL_RESOURCE_PREFIX : RESOURCE_PREFIX) + name;
        InputStream resource = (internal ? ResourceLoader.SYSTEM_CLASS_LOADER : LOADER).getResource(resourceName);
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

    public static ResourceLoader initialize(ResourceLoader loader) {
        if (LOADER == null)
            LOADER = loader;
        return LOADER;
    }
}
