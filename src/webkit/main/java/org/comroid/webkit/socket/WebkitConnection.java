package org.comroid.webkit.socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.MimeType;
import org.comroid.restless.REST;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.restless.server.RestServer;
import org.comroid.restless.socket.WebsocketPacket;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.webkit.config.WebkitConfiguration;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.comroid.webkit.server.WebSocketConnection;
import org.java_websocket.WebSocket;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class WebkitConnection extends WebSocketConnection {
    private static final String CLIENT_HELLO_PREFIX = "hello server; i'm ";
    private static final Logger logger = LogManager.getLogger();
    public final String host;

    public WebkitConnection(
            WebSocket socketBase,
            REST.Header.List headers,
            ContextualProvider context
    ) {
        super(socketBase, headers, context);
        this.host = getHeaders().getFirst("Host");

        RefContainer<WebsocketPacket.Type, WebsocketPacket> listener = on(WebsocketPacket.Type.DATA);
        on(WebsocketPacket.Type.CLOSE).peek(n -> {
            logger.trace("Closing Webkit Listener");
            listener.close();
        });

        listener.flatMap(WebsocketPacket::getData)
                .yield(str -> !str.startsWith(CLIENT_HELLO_PREFIX), str -> {
                    if (handleHello(str.substring(CLIENT_HELLO_PREFIX.length() + 1)))
                        sendText("hello client");
                    else close(1008, "hello impostor; i do not accept u");
                })
                .map(findSerializer()::parse)
                .peek(command -> {
                    UniObjectNode response = findSerializer().createObjectNode().asObjectNode();
                    try {
                        logger.trace("Incoming command: {}", command);
                        String commandStr = command.get("type").asString();
                        String[] split = commandStr.split("/");
                        String commandCategory = split[0];
                        String commandName = split[1];

                        UniNode data = command.wrap("data").orElse(UniValueNode.NULL);
                        Map<String, Object> pageProperties = requireFromContext(PagePropertiesProvider.class).findPageProperties(getHeaders());

                        switch (commandCategory) {
                            case "webkit":
                                switch (commandName) {
                                    case "changePanel":
                                        String target = data.get("target").asString();
                                        pageProperties.put("frame", target);

                                        try (
                                                InputStream is = WebkitConfiguration.get().getPanel(target);
                                                InputStreamReader isr = new InputStreamReader(is);
                                                BufferedReader br = new BufferedReader(isr)
                                        ) {
                                            String panelData = br.lines().collect(Collectors.joining("\n"));
                                            Document doc = Jsoup.parse(panelData);
                                            String docString = FrameBuilder.fabricateDocumentToString(doc, host, pageProperties);

                                            response.put("type", "changePanel");
                                            response.put("data", docString);
                                            break;
                                        } catch (Throwable e) {
                                            logger.error("Could not read target panel " + target, e);
                                        }
                                    case "refresh":
                                        response.put("type", "inject");
                                        UniObjectNode eventData = response.putObject("data");
                                        eventData.putAll(pageProperties);
                                        break;
                                    default:
                                        logger.error("Unknown action: {}", commandName);
                                }
                                break;
                            default:
                                logger.debug("Not a Webkit command; using handler method.");
                                response.put("type", commandName);
                                UniObjectNode responseData = response.putObject("data");
                                handleCommand(pageProperties, commandCategory, commandName, data, responseData);
                                logger.debug("Data after Handler: {}", responseData);
                                break;
                        }
                    } catch (Exception e) {
                        logger.error("Error occurred in command handler", e);
                        response.put("type", "error");
                        RestEndpointException u = new RestEndpointException(HTTPStatusCodes.INTERNAL_SERVER_ERROR, "Error in command handler", e);
                        UniObjectNode errorNode = RestServer.generateErrorNode(upgrade(Context.class), MimeType.JSON, u);
                        response.put("data", errorNode);
                    } finally {
                        sendText(response);
                    }
                });
    }

    protected final Serializer<UniNode> findSerializer() {
        return findSerializer(MimeType.JSON);
    }

    @OverrideOnly
    protected boolean handleHello(String identification) {
        return true;
    }

    protected abstract void handleCommand(
            Map<String, Object> pageProperties,
            String category,
            String name,
            UniNode data,
            UniObjectNode response
    );
}
