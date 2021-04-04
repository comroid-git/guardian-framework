package org.comroid.webkit.endpoint;

import com.sun.net.httpserver.Headers;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.EndpointScope;
import org.comroid.restless.server.EndpointHandler;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.uniform.node.UniNode;
import org.comroid.webkit.frame.FrameBuilder;
import org.intellij.lang.annotations.Language;

import java.io.InputStream;
import java.io.InputStreamReader;

import static org.comroid.restless.HTTPStatusCodes.INTERNAL_SERVER_ERROR;
import static org.comroid.restless.HTTPStatusCodes.OK;

public enum WebkitScope implements EndpointScope, EndpointHandler {
    FRAME("webkit/frame") {
        @Override
        public REST.Response executeGET(Headers headers, String[] requestPath, UniNode body) throws RestEndpointException {
            return new REST.Response(OK, "text/html", new FrameBuilder()

                    .toReader());
        }
    },
    WEBKIT_API("webkit/api") {
        @Override
        public REST.Response executeGET(Headers headers, String[] urlParams, UniNode body) throws RestEndpointException {
            InputStream resource = ClassLoader.getSystemResourceAsStream(FrameBuilder.INTERNAL_RESOURCE_PREFIX + "api.js");
            if (resource == null)
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not find API in resources");
            return new REST.Response(OK, "application/javascript", new InputStreamReader(resource));
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
