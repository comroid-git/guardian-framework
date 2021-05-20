package org.comroid.webkit.endpoint;

import org.comroid.api.os.OS;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.EndpointScope;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.ReaderUtil;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.comroid.webkit.server.WebkitServer;
import org.intellij.lang.annotations.Language;

import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Map;

import static org.comroid.restless.HTTPStatusCodes.INTERNAL_SERVER_ERROR;
import static org.comroid.restless.HTTPStatusCodes.OK;

public enum WebkitScope implements EndpointScope, EndpointHandler {
    FRAME("/webkit/frame") {
        @Override
        public REST.Response executeGET(Context context, URI requestURI, REST.Header.List headers, String[] requestPath, UniNode body) throws RestEndpointException {
            Map<String, Object> pageProperties = context
                    .requireFromContext(PagePropertiesProvider.class)
                    .findPageProperties(headers);

            if (requestPath.length > 1) {
                context.getLogger().debug("Adding request path to page properties");
                String[] args = new String[requestPath.length - 1];
                System.arraycopy(requestPath, 1, args, 0, args.length);
                pageProperties.put("args", Arrays.asList(args));
            }

            String panel = (requestPath.length > 0 && !requestPath[0].isEmpty()) ? requestPath[0] : "main";

            String scheme = requestURI.getScheme();
            boolean secure = scheme != null && scheme.equals("https");
            FrameBuilder frameBuilder = new FrameBuilder(panel, headers, pageProperties, false, secure);
            return new REST.Response(OK, "text/html", frameBuilder.toReader());
        }

        @Override
        public boolean ignoreArgumentCount() {
            return true;
        }
    },
    WEBKIT_API("/webkit/api") {
        @Override
        public REST.Response executeGET(Context context, URI requestURI, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            InputStream resource = FrameBuilder.getInternalResource("api.js");
            if (resource == null)
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not find API in resources");
            Map<String, Object> pageProperties = context
                    .requireFromContext(WebkitServer.class)
                    .findPageProperties(headers);
            UniObjectNode obj = context.createObjectNode();
            obj.putAll(pageProperties);
            return new REST.Response(OK, "application/javascript", ReaderUtil.combine(
                    String.format("isWindows = %s;\nsocketToken = '%s';\nsessionData = JSON.parse('%s');\n",
                            OS.isWindows, headers.tryFirst(CommonHeaderNames.AUTHORIZATION)
                                    .orElseGet(() -> headers.getFirst(CommonHeaderNames.COOKIE)), obj.toSerializedString()),
                    resource));
        }
    };

    private final String extension;
    private final String[] regExp;

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regExp;
    }

    WebkitScope(String extension, @Language("RegExp") String... regExp) {
        this.extension = extension;
        this.regExp = regExp;
    }
}
