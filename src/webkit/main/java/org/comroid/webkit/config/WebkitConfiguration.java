package org.comroid.webkit.config;

import org.comroid.api.ContextualProvider;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.ref.FutureReference;
import org.comroid.uniform.adapter.xml.jsoup.JsoupXmlParser;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;
import org.comroid.webkit.frame.FrameBuilder;
import org.jetbrains.annotations.ApiStatus.Internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("ClassInitializerMayBeStatic")
public final class WebkitConfiguration extends DataContainerBase<WebkitConfiguration> {
    private static final FutureReference<WebkitConfiguration> instance = new FutureReference<>();
    @Internal
    @RootBind
    public static GroupBind<WebkitConfiguration> Type;
    private static VarBind<WebkitConfiguration, UniObjectNode, UniObjectNode, UniObjectNode> PARTS;
    private static VarBind<WebkitConfiguration, UniObjectNode, UniObjectNode, UniObjectNode> PANELS;
    private final Ref<UniObjectNode> parts;
    private final Ref<UniObjectNode> panels;

    private WebkitConfiguration(ContextualProvider context, UniObjectNode data) {
        super(context, data);
        this.parts = getComputedReference(PARTS);
        this.panels = getComputedReference(PANELS);
    }

    public static WebkitConfiguration get() {
        return instance.assertion();
    }

    public static WebkitConfiguration initialize(ContextualProvider context) {
        if (instance.future.isDone() || Type != null)
            throw new IllegalStateException("Configuration is already initialized");
        Type = new GroupBind<>(context, "webkit-configuration");

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
        if (Type == null)
            throw new IllegalStateException("Initialization Process incorrect");
        PARTS = Type.createBind("parts")
                .extractAsObject()
                .build();
        PANELS = Type.createBind("panels")
                .extractAsObject()
                .build();
        UniObjectNode initialData = JsoupXmlParser.instance.parse(data);
        if (!instance.future.complete(new WebkitConfiguration(context, initialData)))
            throw new RuntimeException("Could not initialize Webkit Configuration");
        return instance.assertion("Initialization failed");
    }

    public Stream<String> streamPartNames() {
        return parts.map(obj -> obj.keySet().stream())
                .orElseGet(Stream::empty);
    }

    public InputStream getPart(String name) {
        return parts.map(obj -> obj.get(name))
                .map(UniNode::asString)
                .map(resource -> FrameBuilder.getResource("part/" + resource))
                .assertion("Could not find any part named " + name);
    }

    public Stream<String> streamPanelNames() {
        return panels.map(obj -> obj.keySet().stream())
                .orElseGet(Stream::empty);
    }

    public InputStream getPanel(String name) {
        return panels.map(obj -> obj.get(name))
                .map(UniNode::asString)
                .map(resource -> FrameBuilder.getResource("panel/" + resource))
                .assertion("Could not find any panel named " + name);
    }
}
