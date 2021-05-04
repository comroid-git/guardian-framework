package org.comroid.restless.server;

import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;

import java.lang.reflect.Method;
import java.util.Arrays;

public interface EndpointHandler {
    @Deprecated
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
            Context context,
            REST.Method method,
            REST.Header.List headers,
            String[] urlParams,
            UniNode data
    ) throws RestEndpointException {
        switch (method) {
            case GET:
                return executeGET(context, headers, urlParams, data);
            case PUT:
                return executePUT(context, headers, urlParams, data);
            case POST:
                return executePOST(context, headers, urlParams, data);
            case PATCH:
                return executePATCH(context, headers, urlParams, data);
            case DELETE:
                return executeDELETE(context, headers, urlParams, data);
            case HEAD:
                return executeHEAD(context, headers, urlParams, data);
        }

        throw new AssertionError("No such method: " + method);
    }

    default REST.Response executeGET(
            Context context,
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: GET");
    }

    default REST.Response executePUT(
            Context context,
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PUT");
    }

    default REST.Response executePOST(
            Context context,
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: POST");
    }

    default REST.Response executePATCH(
            Context context,
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PATCH");
    }

    default REST.Response executeDELETE(
            Context context,
            REST.Header.List headers,
            String[] urlParams,
            UniNode body
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: DELETE");
    }

    default REST.Response executeHEAD(
            Context context,
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
        default REST.Response executeMethod(Context context, REST.Method method, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            return getEndpointHandler().executeMethod(context, method, headers, urlParams, body);
        }
    }
}
