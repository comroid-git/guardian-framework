package org.comroid.webkit.socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Serializer;
import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.ref.Reference;
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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class WebkitConnection extends WebSocketConnection {
    private static final Logger logger = LogManager.getLogger();
    public final String host;
    private final Ref<Map<String, Object>> properties = Reference.provided(() ->
            requireFromContext(PagePropertiesProvider.class).findPageProperties(getHeaders()));

    protected Map<String, Object> getPageProperties() {
        return properties.assertion();
    }

    public WebkitConnection(
            WebSocket socketBase,
            REST.Header.List headers,
            ContextualProvider context
    ) {
        super(socketBase, headers, context);
        this.host = getHeaders().getFirst("Host");

        on(WebsocketPacket.Type.DATA)
                .flatMap(WebsocketPacket::getData)
                .yield(str -> !str.startsWith("hello"), str -> sendToPanel("home"))
                .map(findSerializer()::parse)
                .peek(this::handleCommand);
    }

    protected void sendToPanel(String targetPanel) {
        UniObjectNode response = findSerializer().createObjectNode().asObjectNode();
        Map<String, Object> pageProperties = getPageProperties();
        intoChangePanelCommand(response, targetPanel, pageProperties);
        sendText(response);
    }

    private void handleCommand(UniNode command) {
        UniObjectNode response = findSerializer().createObjectNode().asObjectNode();
        try {
            dispatchCommand(command, response);
        } catch (Exception e) {
            logger.error("Error occurred in command handler", e);
            response.put("type", "error");
            RestEndpointException u = new RestEndpointException(HTTPStatusCodes.INTERNAL_SERVER_ERROR, "Error in command handler", e);
            UniObjectNode errorNode = RestServer.generateErrorNode(upgrade(Context.class), MimeType.JSON, u);
            response.put("data", errorNode);
        } finally {
            sendText(response);
        }
    }

    private void dispatchCommand(UniNode command, UniObjectNode response) {
        logger.trace("Incoming command: {}", command);
        String commandStr = command.get("type").asString();
        String[] split = commandStr.split("/");
        String commandCategory = split[0];
        String commandName = split[1];

        UniNode data = command.wrap("data").orElse(UniValueNode.NULL);
        Map<String, Object> pageProperties = getPageProperties();

        switch (commandCategory) {
            case "webkit":
                switch (commandName) {
                    case "changePanel":
                        intoChangePanelCommand(response, data.get("target").asString(), pageProperties);
                        break;
                    case "refresh":
                        intoRefreshCommand(response, pageProperties);
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
    }

    private void intoChangePanelCommand(UniObjectNode response, String target, Map<String, Object> pageProperties) {
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
            return;
        } catch (Throwable e) {
            logger.error("Could not read target panel " + target, e);
        }
    }

    private void intoRefreshCommand(UniObjectNode response, Map<String, Object> pageProperties) {
        response.put("type", "inject");
        UniObjectNode eventData = response.putObject("data");
        eventData.putAll(pageProperties);
    }

    protected final Serializer<UniNode> findSerializer() {
        return findSerializer(MimeType.JSON);
    }

    protected abstract void handleCommand(
            Map<String, Object> pageProperties,
            String category,
            String name,
            UniNode data,
            UniObjectNode response
    );
}
