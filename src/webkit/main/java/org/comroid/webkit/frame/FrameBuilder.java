package org.comroid.webkit.frame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Builder;
import org.comroid.api.PropertiesHolder;
import org.comroid.api.Rewrapper;
import org.comroid.api.StringSerializable;
import org.comroid.api.os.OS;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.webkit.config.WebkitConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.script.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FrameBuilder implements Builder<Document>, StringSerializable, PropertiesHolder {
    public static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\[([\\w\\d\\S.]+?)]");
    public static final String RESOURCE_PREFIX = "org.comroid.webkit/";
    public static final String INTERNAL_RESOURCE_PREFIX = "org.comroid.webkit.internal/";
    public static final Reference<ClassLoader> classLoader;
    private static final Logger logger;
    private static final Map<String, String> frameCache;
    private static final Map<String, String> partCache;
    private static final Map<String, String> panelCache;
    private static final Reference<ScriptEngine> jsEngine;

    static {
        logger = LogManager.getLogger();
        frameCache = new ConcurrentHashMap<>();
        partCache = new ConcurrentHashMap<>();
        panelCache = new ConcurrentHashMap<>();
        classLoader = Reference.create(ClassLoader.getSystemClassLoader());
        jsEngine = classLoader.map(loader -> new ScriptEngineManager(loader).getEngineByExtension("js"));
    }

    public final String host;
    private final Document frame;
    private final Map<String, Object> pageProperties;
    private final boolean isError;
    private @Nullable String panel = "home";

    public @Nullable String getPanel() {
        return panel;
    }

    public void setPanel(@Nullable String panel) {
        this.panel = panel;
    }

    public FrameBuilder(REST.Header.List headers, Map<String, Object> pageProperties) {
        this("main", headers, pageProperties, false);
    }

    public FrameBuilder(String frameName, REST.Header.List headers, Map<String, Object> pageProperties, boolean isError) {
        boolean isDebug = OS.isWindows; // fixme Wrong isDebug check
        this.isError = isError;
        pageProperties.put("frame", frameName);
        this.pageProperties = pageProperties;

        try {
            this.frame = Jsoup.parse(findFrameData(frameName));
        } catch (Throwable e) {
            throw new RuntimeException("Could not load page frame", e);
        }

        host = headers.getFirst("Host");
        logger.info("Initializing new FrameBuilder for Host {} with {} props", host, pageProperties.size());
        logger.trace("FrameBuilder has properties:\n{}", pageProperties.entrySet()
                .stream()
                .map(entry -> String.format("%s -> %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n")));

        // add api script
        frame.head().appendElement("script")
                .attr("type", "application/javascript")
                .attr("src", String.format("http%s://%s/webkit/api", isDebug ? "" : "s", host));
        frame.body().attr("onload", "initAPI()");
        frame.body().attr("onclose", "disconnectAPI()");
    }

    public static @NotNull InputStream getInternalResource(String name) {
        return getResource(true, name);
    }

    public static @NotNull InputStream getResource(String name) {
        return getResource(false, name);
    }

    public static @NotNull InputStream getResource(boolean internal, String name) {
        String resourceName = (internal ? INTERNAL_RESOURCE_PREFIX : RESOURCE_PREFIX) + name;
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

    public static String fabricateDocumentToString(Document frame, String host, Map<String, Object> pageProperties) {
        fabricateDocument(frame, host, pageProperties);
        return postfabString(pageProperties, frame.toString());
    }

    private static void fabricateDocument(Document frame, String host, Map<String, Object> pageProperties) {
        WebkitConfiguration config = WebkitConfiguration.get();
        // read parts
        config.streamPartNames()
                .forEach(part -> {
                    String partData = findPartData(part);

                    // and import to frame
                    frame.getElementsByTag(part).html(partData);
                });

        // overwrite links
        frame.getElementsByTag("a")
                .forEach(dom -> {
                    boolean isDebug = OS.isWindows; // fixme Wrong isDebug check
                    String href = dom.attr("href");
                    if (href.isEmpty())
                        return;
                    if (href.startsWith("http")) {
                        if (!isDebug)
                            dom.attr(href, href.replace("http://", "https://"));
                        return;
                    }
                    if (href.startsWith("~/")) {
                        dom.attr("href", href.replace("~/", String.format("http%s://%s/", isDebug ? "" : "s", host)));
                        return;
                    }
                    dom.removeAttr("href");
                    dom.attr("onclick", String.format("actionChangePanel('%s')", href));
                });

        // apply panel attributes
        frame.getElementsByAttribute("panel")
                .forEach(dom -> {
                    String panelName = dom.attr("panel");
                    String panelData = findPanelData(panelName);

                    dom.html(panelData);
                });

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
                    dom.attr("style", style
                            + (style.isEmpty() || !style.contains("\n") ? "" : '\n')
                            + "cursor: pointer !important;");
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

    }

    private static String resolveValue(Map<String, Object> stage, String[] path, int index) {
        Object it = stage.get(path[index]);
        if (index + 1 >= path.length || it instanceof String)
            return String.valueOf(it);
        //noinspection unchecked
        return resolveValue((Map<String, Object>) it, path, index + 1);
    }

    private static String findFrameData(String frame) {
        return findAndCacheResourceData(frame, frameCache, () -> WebkitConfiguration.get().getFrame(frame));
    }

    private static String findPartData(String part) {
        return findAndCacheResourceData(part, partCache, () -> WebkitConfiguration.get().getPart(part));
    }

    private static String findPanelData(String panel) {
        return findAndCacheResourceData(panel, panelCache, () -> WebkitConfiguration.get().getPanel(panel));
    }

    private static String postfabString(final Map<String, Object> pageProperties, String untreated) {
        boolean isDebug = OS.isWindows; // fixme Wrong isDebug check

        // fill in vars
        Matcher matcher = VARIABLE_PATTERN.matcher(untreated);
        while (matcher.find()) {
            String vname = matcher.group(1);
            String value = resolveValue(pageProperties, vname.split("\\."), 0);
            MatchResult result = matcher.toMatchResult();
            untreated = untreated.substring(0, result.start()) + value + untreated.substring(result.end());
        }

        // replace ~ with http
        untreated = untreated.replace("~/", isDebug ? "http://" : "https://");

        return untreated;
    }

    @Override
    public Document build() {
        Objects.requireNonNull(panel, "No Panel defined");

        if (isError) {
            logger.debug("Building Error Frame with PageProperties {}", pageProperties);
            String errorPanel = findAndCacheResourceData("error", panelCache, () -> getInternalResource("error.html"));
            frame.getElementById("content").html(errorPanel);
        } else if (!frame.select("[id='content']").isEmpty()) {
            logger.debug("Building Frame with panel {}", panel);
            frame.getElementById("content").html(findPanelData(panel));
        }

        fabricateDocument(frame, host, pageProperties);

        return frame;
    }

    @Override
    public String toSerializedString() {
        return postfabString(pageProperties, build().toString());
    }

    @Override
    public final <T> Rewrapper<T> getProperty(String name) {
        if (pageProperties.containsKey(name))
            //noinspection unchecked
            return () -> (T) pageProperties.get(name);
        return Reference.empty();
    }

    @Override
    public final boolean setProperty(String name, Object value) {
        return pageProperties.put(name, value) != value;
    }
}
