package org.comroid.restless.server;

import org.comroid.api.ContextualProvider;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.uniform.node.UniNode;

import java.lang.reflect.Method;
import java.util.Arrays;

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
            REST.Header.List headers,
            String[] urlParams,
            UniNode data
    ) throws RestEndpointException {
        if (!supports(method))
            throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: " + method.name());

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
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: GET");
    }

    default REST.Response executePUT(
            ContextualProvider context,
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PUT");
    }

    default REST.Response executePOST(
            ContextualProvider context,
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: POST");
    }

    default REST.Response executePATCH(
            ContextualProvider context,
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PATCH");
    }

    default REST.Response executeDELETE(
            ContextualProvider context,
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: DELETE");
    }

    default REST.Response executeHEAD(
            ContextualProvider context,
            REST.Header.List headers,
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
        default REST.Response executeMethod(RestServer server, REST.Method method, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return getEndpointHandler().executeMethod(server, method, headers, urlParams, body);
        }
    }
}
