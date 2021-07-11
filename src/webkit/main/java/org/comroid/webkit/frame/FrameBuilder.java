package org.comroid.webkit.frame;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.*;
import org.comroid.api.os.OS;
import org.comroid.mutatio.ref.Reference;
import org.comroid.restless.REST;
import org.comroid.webkit.config.WebkitConfiguration;
import org.comroid.webkit.config.WebkitResourceLoader;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.script.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class FrameBuilder implements Builder<Document>, StringSerializable, PropertiesHolder, ContextualProvider.Underlying {
    public static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\[([\\w\\d\\S.]+?)]");
    public static final Reference<ClassLoader> classLoader;
    private static final Logger logger;
    private static final Reference<ScriptEngine> jsEngine;

    static {
        logger = LogManager.getLogger();
        classLoader = Reference.create(ClassLoader.getSystemClassLoader());
        jsEngine = classLoader.map(loader -> new ScriptEngineManager(loader).getEngineByExtension("js"));
    }

    public final String host;
    private final ContextualProvider context;
    private final Document frame;
    private final Map<String, Object> pageProperties;
    private final boolean isError;
    private @Nullable String panel = "home";

    public @Nullable String getPanel() {
        return panel;
    }

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public void setPanel(@Nullable String panel) {
        this.panel = panel;
    }

    @Deprecated
    public FrameBuilder(ContextualProvider context, REST.Header.List headers) {
        this(context, "main", headers, false, true);
    }

    public FrameBuilder(ContextualProvider context, String frameName, REST.Header.List headers, boolean isError) {
        this(context, frameName, headers, isError, true);
    }

    public FrameBuilder(ContextualProvider context, String frameName, REST.Header.List headers, boolean isError, boolean isSecure) {
        this.context = context;
        boolean isDebug = OS.isWindows; // fixme Wrong isDebug check
        this.isError = isError;
        this.pageProperties = context.requireFromContext(PagePropertiesProvider.class).findPageProperties(headers);
        pageProperties.put("frame", frameName);

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
                .attr("src", "https://raw.githubusercontent.com/comroid-git/guardian-framework/master/src/webkit/main/resources/org/comroid/webkit/internal/api.js");
        frame.body().attr("onload", "initAPI()");
        frame.body().attr("onclose", "disconnectAPI()");
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

        fabricate$overwriteLinks(frame, host);
        fabricate$applyPanels(frame);
        fabricate$applyWhenAttributes(frame, pageProperties);
        fabricate$applyActionDeclarations(frame);
        fabricate$applyInjection(frame, pageProperties);
    }

    private static void fabricate$applyInjection(Document frame, Map<String, Object> pageProperties) {
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

    private static void fabricate$applyActionDeclarations(Document frame) {
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
    }

    private static void fabricate$applyWhenAttributes(Document frame, Map<String, Object> pageProperties) {
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
                            logger.warn("Attribute evaluation failed; removing " + dom, e);
                            return false;
                        }
                    }).flatMap(Boolean.class).orElse(false);
                    if (!keep)
                        dom.remove();
                    dom.removeAttr("when");
                });
    }

    private static void fabricate$applyPanels(Document frame) {
        // apply panel attributes
        frame.getElementsByAttribute("panel")
                .forEach(dom -> {
                    String panelName = dom.attr("panel");
                    String panelData = findPanelData(panelName);

                    dom.html(panelData);
                });
    }

    private static void fabricate$overwriteLinks(Document frame, String host) {
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
    }

    private static String resolveValue(Map<String, Object> stage, String[] path, int index) {
        Object it = stage.get(path[index]);
        if (index + 1 >= path.length || it instanceof String)
            return String.valueOf(it);
        //noinspection unchecked
        return resolveValue((Map<String, Object>) it, path, index + 1);
    }

    private static String findFrameData(String frame) {
        return readResource(WebkitConfiguration.get().getFrame(frame));
    }

    private static String findPartData(String part) {
        return readResource(WebkitConfiguration.get().getPart(part));
    }

    private static String findPanelData(String panel) {
        return readResource(WebkitConfiguration.get().getPanel(panel));
    }

    private static String readResource(InputStream resource) {
        String data;
        try (
                InputStreamReader isr = new InputStreamReader(resource);
                BufferedReader br = new BufferedReader(isr)
        ) {
            data = br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Could not read resource");
        }
        return data;
    }

    public static String postfabString(final Map<String, Object> pageProperties, String untreated) {
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
            String errorPanel = findPanelData("error");
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
