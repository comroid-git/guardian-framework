package org.comroid.webkit.config;

import org.comroid.api.ContextualProvider;
import org.comroid.mutatio.ref.FutureReference;
import org.comroid.uniform.adapter.xml.jsoup.JsoupXmlParser;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
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
import java.util.stream.Collectors;

@SuppressWarnings("ClassInitializerMayBeStatic")
public final class WebkitConfiguration extends DataContainerBase<WebkitConfiguration> {
    private static final FutureReference<WebkitConfiguration> instance = new FutureReference<>();
    @Internal
    @RootBind
    public static GroupBind<WebkitConfiguration> Type;
    public static VarBind<WebkitConfiguration, String, String, String> BASE_URL;

    {
        if (Type == null)
            throw new IllegalStateException("Initialization Process incorrect");
        BASE_URL = Type.createBind("baseUrl")
                .extractAs(StandardValueType.STRING)
                .build();
    }

    private WebkitConfiguration(ContextualProvider context, UniObjectNode data) {
        super(context, data);
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
        UniObjectNode initialData = JsoupXmlParser.instance.parse(data);
        if (!instance.future.complete(new WebkitConfiguration(context, initialData)))
            throw new RuntimeException("Could not initialize Webkit Configuration");
        return instance.assertion("Initialization failed");
    }
}
