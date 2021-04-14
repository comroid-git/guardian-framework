package org.comroid.webkit.endpoint;

import com.sun.net.httpserver.Headers;
import org.comroid.api.ContextualProvider;
import org.comroid.api.os.OS;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.EndpointScope;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.uniform.node.UniNode;
import org.comroid.util.ReaderUtil;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.intellij.lang.annotations.Language;

import java.io.InputStream;
import java.util.Map;

import static org.comroid.restless.HTTPStatusCodes.INTERNAL_SERVER_ERROR;
import static org.comroid.restless.HTTPStatusCodes.OK;

public enum WebkitScope implements EndpointScope, EndpointHandler {
    FRAME("webkit/frame") {
        @Override
        public REST.Response executeGET(ContextualProvider context, Headers headersJ, String[] requestPath, UniNode body) throws RestEndpointException {
            REST.Header.List headers = REST.Header.List.of(headersJ);
            Map<String, Object> pageProperties = context
                    .requireFromContext(PagePropertiesProvider.class)
                    .findPageProperties(headers);

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
    WEBKIT_API("webkit/api") {
        @Override
        public REST.Response executeGET(ContextualProvider context, Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            InputStream resource = ClassLoader.getSystemResourceAsStream(FrameBuilder.INTERNAL_RESOURCE_PREFIX + "api.js");
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
