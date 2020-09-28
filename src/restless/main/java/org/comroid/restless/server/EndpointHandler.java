package org.comroid.restless.server;

import com.sun.net.httpserver.Headers;
import org.comroid.mutatio.proc.Processor;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;

import java.util.stream.Stream;

public interface EndpointHandler {
    default boolean supports(REST.Method method) {
        try {
            return !getClass().getMethod("execute" + method.name(), Headers.class, String[].class, UniNode.class)
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
                : Processor.ofConstant(server.getSerializationAdapter().createUniNode(body))
                .orElseGet(() -> {
                    // try to wrap http form data
                    try {
                        UniObjectNode node = server.getSerializationAdapter().createUniObjectNode();
                        Stream.of(body.split("&"))
                                .map(pair -> pair.split("="))
                                .forEach(field -> node.put(field[0], ValueType.STRING, field[1]));
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
