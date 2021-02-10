package org.comroid.restless.server;

import com.sun.net.httpserver.Headers;
import org.comroid.api.Rewrapper;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.impl.AbstractUniNode;
import org.comroid.util.StandardValueType;

import java.util.stream.Stream;

public interface EndpointHandler {
    default boolean supports(REST.Method method) {
        try {
            return !getClass().getMethod("execute" + method.name(), Headers.class, String[].class, AbstractUniNode.class)
                    .getDeclaringClass()
                    .equals(EndpointHandler.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
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

        final UniNode data = body.isEmpty() ? null
                : Rewrapper.of(server.getSerializationAdapter().createUniNode(body))
                .orElseGet(() -> {
                    // try to wrap http form data
                    try {
                        UniObjectNode node = server.getSerializationAdapter().createObjectNode();
                        Stream.of(body.split("&"))
                                .map(pair -> pair.split("="))
                                .forEach(field -> node.put(field[0], StandardValueType.STRING, field[1]));
                        return node;
                    } catch (Throwable ignored) {
                        return null;
                    }
                });

        switch (method) {
            case GET:
                return executeGET(headers, urlParams, data);
            case PUT:
                return executePUT(headers, urlParams, data);
            case POST:
                return executePOST(headers, urlParams, data);
            case PATCH:
                return executePATCH(headers, urlParams, data);
            case DELETE:
                return executeDELETE(headers, urlParams, data);
            case HEAD:
                return executeHEAD(headers, urlParams, data);
        }

        throw new AssertionError("No such method: " + method);
    }

    default REST.Response executeGET(
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: GET");
    }

    default REST.Response executePUT(
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PUT");
    }

    default REST.Response executePOST(
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: POST");
    }

    default REST.Response executePATCH(
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PATCH");
    }

    default REST.Response executeDELETE(
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: DELETE");
    }

    default REST.Response executeHEAD(
            Headers headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: HEAD");
    }
}
