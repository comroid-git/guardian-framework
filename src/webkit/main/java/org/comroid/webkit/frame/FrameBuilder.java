package org.comroid.webkit.frame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Builder;
import org.comroid.api.StringSerializable;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.webkit.config.WebkitConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import javax.script.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class FrameBuilder implements Builder<Document>, StringSerializable {
    public static final String RESOURCE_PREFIX = "org.comroid.webkit/";
    public static final String INTERNAL_RESOURCE_PREFIX = "org.comroid.webkit.internal/";
    public static final Reference<ClassLoader> classLoader;
    private static final Logger logger;
    private static final Map<String, String> partCache;
    private static final Map<String, String> panelCache;
    private static final Reference<ScriptEngine> jsEngine;

    static {
        logger = LogManager.getLogger();
        partCache = new ConcurrentHashMap<>();
        panelCache = new ConcurrentHashMap<>();
        classLoader = Reference.create(ClassLoader.getSystemClassLoader());
        jsEngine = classLoader.map(loader -> new ScriptEngineManager(loader).getEngineByExtension("js"));
    }

    public final String host;
    private final Document frame;
    private final Map<String, Object> pageProperties;
    private @Nullable String panel = "home";

    public @Nullable String getPanel() {
        return panel;
    }

    public void setPanel(@Nullable String panel) {
        this.panel = panel;
    }

    public FrameBuilder(REST.Header.List headers, Map<String, Object> pageProperties) {
        this.pageProperties = pageProperties;
        try {
            InputStream frame = getResource("frame.html");
            this.frame = Jsoup.parse(frame, "UTF-8", "");
        } catch (Throwable e) {
            throw new RuntimeException("Could not load page frame", e);
        }

        host = headers.getFirst("Host");
        logger.info("Initializing new FrameBuilder for Host {} with {} props", host, pageProperties);

        // add api script
        frame.head().appendElement("script")
                .attr("type", "application/javascript")
                .attr("src", String.format("http://%s/webkit/api", host));
        frame.body().attr("onload", "initAPI()");
        frame.body().attr("onclose", "disconnectAPI()");

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
        InputStream resource = classLoader.map(loader -> loader.getResourceAsStream(resourceName)).get();
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

    private static String findAndCacheResourceData(String key, Map<String, String> cache, Supplier<InputStream> resource) {
        return cache.computeIfAbsent(key, k -> {
            String data;
            try (
                    InputStream in = resource.get();
                    InputStreamReader isr = new InputStreamReader(in);
                    BufferedReader br = new BufferedReader(isr)
            ) {
                data = br.lines().collect(Collectors.joining("\n"));
            } catch (IOException e) {
                throw new RuntimeException("Could not read resource data for member resource: " + key);
            }
            return data;
        });
    }

    public static void fabricateDocument(Document frame, String host, Map<String, Object> pageProperties) {
        // apply when-attributes
        frame.getElementsByAttribute("when")
                .forEach(dom -> {
                    String script = dom.attr("when");
                    boolean keep = jsEngine.peek(engine -> {
                        Bindings bindings = engine.createBindings();
                        bindings.putAll(pageProperties);
                        engine.setBindings(bindings, ScriptContext.GLOBAL_SCOPE);
                    }).map(engine -> {
                        try {
                            return engine.eval(script);
                        } catch (ScriptException e) {
                            throw new RuntimeException("Error in attribute evaluation", e);
                        }
                    }).flatMap(Boolean.class).orElse(false);
                    if (!keep)
                        dom.remove();
                    dom.removeAttr("when");
                });

        // apply action declarations
        frame.getElementsByAttribute("command")
                .forEach(dom -> {
                    String command = dom.attr("command");

                    dom.attr("onclick", String.format("sendCommand('%s')", command));
                    String style = dom.attr("style");
                    dom.attr("style", style + "\ncursor: pointer !important;");
                });

        // try apply value injections
        frame.getElementsByAttribute("inject")
                .forEach(dom -> {
                    String fname = dom.attr("inject");
                    try {
                        String[] path = fname.split("\\.");
                        String value = resolveValue(pageProperties, path, 0);

                        dom.html(value);
                    } catch (Throwable t) {
                        logger.error("Error when injecting value " + fname, t);
                        dom.html("NULL");
                    }
                });

        // overwrite links
        frame.getElementsByTag("a")
                .forEach(dom -> {
                    String href = dom.attr("href");
                    if (href.startsWith("http"))
                        return;
                    if (href.startsWith("~/")) {
                        dom.attr("href", href.replace("~/", String.format("http://%s/", host)));
                        return;
                    }
                    dom.removeAttr("href");
                    dom.attr("onclick", String.format("actionChangePanel('%s')", href));
                });
    }

    private static String resolveValue(Map<String, Object> stage, String[] path, int index) {
        Object it = stage.get(path[index]);
        if (index + 1 >= path.length || it instanceof String)
            return String.valueOf(it);
        //noinspection unchecked
        return resolveValue((Map<String, Object>) it, path, index + 1);
    }

    private String findPartData(String part) {
        return findAndCacheResourceData(part, partCache, () -> WebkitConfiguration.get().getPart(part));
    }

    private String findPanelData(String panel) {
        return findAndCacheResourceData(panel, panelCache, () -> WebkitConfiguration.get().getPanel(panel));
    }

    public Document build() {
        Objects.requireNonNull(panel, "No Panel defined");

        logger.debug("Building Frame with panel {}", panel);

        frame.getElementById("content").html(findPanelData(panel));

        fabricateDocument(frame, host, pageProperties);

        return frame;
    }

    @Override
    public String toSerializedString() {
        return build().toString();
    }
}
