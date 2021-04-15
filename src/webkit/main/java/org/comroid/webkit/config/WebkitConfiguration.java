package org.comroid.webkit.config;

import org.comroid.api.ContextualProvider;
import org.comroid.mutatio.ref.FutureReference;
import org.comroid.webkit.frame.FrameBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class WebkitConfiguration implements ContextualProvider.Underlying {
    private static final FutureReference<WebkitConfiguration> instance = new FutureReference<>();
    private final ContextualProvider context;
    private final Map<String, String> frames = new ConcurrentHashMap<>();
    private final Map<String, String> parts = new ConcurrentHashMap<>();
    private final Map<String, String> panels = new ConcurrentHashMap<>();

    @Override
    public ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    private WebkitConfiguration(ContextualProvider context, Document dom) {
        this.context = context.plus("Webkit Configuration", this);

        dom.getElementsByTag("frames")
                .first()
                .children()
                .forEach(element -> frames.put(element.tagName(), element.html()));
        dom.getElementsByTag("parts")
                .first()
                .children()
                .forEach(element -> parts.put(element.tagName(), element.html()));
        dom.getElementsByTag("panels")
                .first()
                .children()
                .forEach(element -> panels.put(element.tagName(), element.html()));
    }

    public static WebkitConfiguration get() {
        return instance.assertion();
    }

    public static WebkitConfiguration initialize(ContextualProvider context) {
        if (instance.future.isDone())
            throw new IllegalStateException("Configuration is already initialized");

        InputStream config = FrameBuilder.getResource("config.xml");
        String data;
        try (
                InputStreamReader isr = new InputStreamReader(config);
                BufferedReader br = new BufferedReader(isr)
        ) {
            data = br.lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            throw new RuntimeException("Could not find config.xml resource (" + FrameBuilder.RESOURCE_PREFIX + "config.xml)", e);
        }
        Document dom = Jsoup.parse(data, "", Parser.xmlParser());
        if (!instance.future.complete(new WebkitConfiguration(context, dom)))
            throw new RuntimeException("Could not initialize Webkit Configuration");

        return instance.assertion("Initialization failed");
    }

    public Stream<String> streamFrameNames() {
        return frames.keySet().stream();
    }

    public InputStream getFrame(String name) {
        if (!frames.containsKey(name))
            throw new NoSuchElementException("Could not find frame with name " + name);
        String resource = frames.get(name);
        return FrameBuilder.getResource(resource);
    }

    public Stream<String> streamPartNames() {
        return parts.keySet().stream();
    }

    public InputStream getPart(String name) {
        if (!parts.containsKey(name))
            throw new NoSuchElementException("Could not find part with name " + name);
        String resource = parts.get(name);
        return FrameBuilder.getResource(resource);
    }

    public Stream<String> streamPanelNames() {
        return panels.keySet().stream();
    }

    public InputStream getPanel(String name) {
        if (!panels.containsKey(name))
            throw new NoSuchElementException("Could not find panel with name " + name);
        String resource = panels.get(name);
        return FrameBuilder.getResource(resource);
    }
}
