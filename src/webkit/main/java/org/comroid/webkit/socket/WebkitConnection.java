package org.comroid.webkit.socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.restless.REST;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.uniform.node.UniNode;
import org.comroid.util.StandardValueType;
import org.comroid.webkit.config.WebkitConfiguration;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.comroid.webkit.server.WebSocketConnection;
import org.java_websocket.WebSocket;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

public class WebkitConnection extends WebSocketConnection {
    private static final Logger logger = LogManager.getLogger();
    public final String host;

    public WebkitConnection(
            WebSocket socketBase,
            REST.Header.List headers,
            ContextualProvider context
    ) {
        super(socketBase, headers, context);
        this.host = getHeaders().getFirst("Host");

        on(WebsocketPacket.Type.DATA)
                .flatMap(WebsocketPacket::getData)
                .yield(str -> !str.startsWith("hello"), str -> sendText("hello client"))
                .map(findSerializer()::parse)
                .peek(command -> {
                    try {
                        handleCommand(command);
                    } catch (Exception e) {
                        logger.error("Error ocurred in command handler", e);
                    }
                });
    }

    private Serializer<UniNode> findSerializer() {
        return findSerializer("application/json");
    }

    private void handleCommand(UniNode command) {
        logger.trace("Incoming command: {}", command);
        String commandName = command.get("type").asString();
        String[] split = commandName.split("/");

        UniNode data = command.get("data");
        UniNode response = findSerializer().createObjectNode();

        switch (split[0]) {
            case "action":
                switch (split[1]) {
                    case "changePanel":
                        String target = data.get("target").asString();
                        try (
                                InputStream is = WebkitConfiguration.get().getPanel(target);
                                InputStreamReader isr = new InputStreamReader(is);
                                BufferedReader br = new BufferedReader(isr)
                        ) {
                            String panelData = br.lines().collect(Collectors.joining("\n"));
                            Document doc = Jsoup.parse(panelData);
                            Map<String, Object> pageProps = requireFromContext(PagePropertiesProvider.class)
                                    .findPageProperties(getHeaders());
                            FrameBuilder.fabricateDocument(doc, host, pageProps);

                            response.put("type", StandardValueType.STRING, "changePanel");
                            response.put("data", StandardValueType.STRING, doc.toString());
                            break;
                        } catch (Throwable e) {
                            logger.error("Could not read target panel " + target, e);
                        }
                    default:
                        logger.error("Unknown action: {}", split[1]);
                }
                break;
            default:
                logger.error("Unknown Command received: {}", commandName);
                break;
        }

        sendText(response);
    }
}
