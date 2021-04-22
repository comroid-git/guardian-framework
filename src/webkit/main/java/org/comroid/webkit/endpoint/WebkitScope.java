package org.comroid.webkit.endpoint;

import org.comroid.api.os.OS;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.EndpointScope;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.util.ReaderUtil;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.intellij.lang.annotations.Language;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static org.comroid.restless.HTTPStatusCodes.INTERNAL_SERVER_ERROR;
import static org.comroid.restless.HTTPStatusCodes.OK;

public enum WebkitScope implements EndpointScope, EndpointHandler {
    FRAME("/webkit/frame") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] requestPath, UniNode body) throws RestEndpointException {
            Map<String, Object> pageProperties = context
                    .requireFromContext(PagePropertiesProvider.class)
                    .findPageProperties(headers);

            if (requestPath.length > 2) {
                context.getLogger().debug("Adding request path to page properties");
                String[] args = new String[requestPath.length - 2];
                System.arraycopy(requestPath, 2, args, 0, args.length);
                pageProperties.put("args", Arrays.asList(args));
            }

            FrameBuilder frameBuilder = new FrameBuilder(headers, pageProperties);
            if (requestPath.length > 0 && !requestPath[0].isEmpty())
                frameBuilder.setPanel(requestPath[0]);
            return new REST.Response(OK, "text/html", frameBuilder.toReader());
        }

        @Override
        public boolean ignoreArgumentCount() {
            return true;
        }
    },
    WEBKIT_API("/webkit/api") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            InputStream resource = FrameBuilder.getInternalResource("api.js");
            if (resource == null)
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not find API in resources");
            return new REST.Response(OK, "application/javascript", ReaderUtil.combine(
                    String.format("isWindows = %s;\nsocketToken = '%s';\n", OS.isWindows, ""),
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
