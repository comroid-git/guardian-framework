package org.comroid.restless.server;

import com.sun.net.httpserver.Headers;
import org.comroid.api.ContextualProvider;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Stream;

public interface EndpointHandler {
    default boolean supports(REST.Method method) {
        final String mName = "execute" + method.name();
        return Arrays.stream(getClass().getMethods())
                .filter(mtd -> mtd.getName().equals(mName))
                .findAny()
                .map(Method::getDeclaringClass)
                .filter(cls -> !cls.equals(EndpointHandler.class))
                .isPresent();
    }

    default REST.Response executeMethod(
            RestServer server,
            REST.Method method,
            Headers headers,
            String[] urlParams,
            String body
    ) throws RestEndpointException {
        if (!supports(method))
            throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: " + method.name());

        UniNode data;
        if (body.isEmpty()) data = null;
        else {
            try {
                data = server.getSerializationAdapter().createUniNode(body);
            } catch (Throwable initial) {
                try {
                    UniObjectNode obj = server.getSerializationAdapter().createObjectNode();
                    Stream.of(body.split("&"))
                            .map(pair -> pair.split("="))
                            .forEach(field -> obj.put(field[0], field[1]));
                    data = obj;
                } catch (Throwable ignored) {
                    throw new RuntimeException("Could not handle endpoint; failed to parse form data", initial);
                }
            }
        }

        switch (method) {
            case GET:
                return executeGET(server.getContext(), headers, urlParams, data);
            case PUT:
                return executePUT(server.getContext(), headers, urlParams, data);
            case POST:
                return executePOST(server.getContext(), headers, urlParams, data);
            case PATCH:
                return executePATCH(server.getContext(), headers, urlParams, data);
            case DELETE:
                return executeDELETE(server.getContext(), headers, urlParams, data);
            case HEAD:
                return executeHEAD(server.getContext(), headers, urlParams, data);
        }

        throw new AssertionError("No such method: " + method);
    }

    default REST.Response executeGET(
            ContextualProvider context,
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: GET");
    }

    default REST.Response executePUT(
            ContextualProvider context,
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PUT");
    }

    default REST.Response executePOST(
            ContextualProvider context,
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: POST");
    }

    default REST.Response executePATCH(
            ContextualProvider context,
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PATCH");
    }

    default REST.Response executeDELETE(
            ContextualProvider context,
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: DELETE");
    }

    default REST.Response executeHEAD(
            ContextualProvider context,
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: HEAD");
    }

    interface Underlying extends EndpointHandler {
        default EndpointHandler getEndpointHandler() {
            return this;
        }

        @Override
        default boolean supports(REST.Method method) {
            return getEndpointHandler().supports(method);
        }

        @Override
        default REST.Response executeMethod(RestServer server, REST.Method method, Headers headers, String[] urlParams, String body) throws RestEndpointException {
            return getEndpointHandler().executeMethod(server, method, headers, urlParams, body);
        }
    }
}
