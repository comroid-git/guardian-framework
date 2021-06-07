package org.comroid.restless.server;

import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.exception.RestEndpointException;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;

import java.lang.reflect.Method;
import java.net.URI;
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
            URI requestURI,
            REST.Request<UniNode> request,
            String[] urlParams
    ) throws RestEndpointException {
        switch (request.getMethod()) {
            case GET:
                return executeGET(context, requestURI, request, urlParams);
            case PUT:
                return executePUT(context, requestURI, request, urlParams);
            case POST:
                return executePOST(context, requestURI, request, urlParams);
            case PATCH:
                return executePATCH(context, requestURI, request, urlParams);
            case DELETE:
                return executeDELETE(context, requestURI, request, urlParams);
            case HEAD:
                return executeHEAD(context, requestURI, request, urlParams);
        }

        throw new AssertionError("Invalid Request: " + request);
    }

    default REST.Response executeGET(
            Context context,
            URI requestURI,
            REST.Request<UniNode> request,
            String[] urlParams
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: GET");
    }

    default REST.Response executePUT(
            Context context,
            URI requestURI,
            REST.Request<UniNode> request,
            String[] urlParams
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PUT");
    }

    default REST.Response executePOST(
            Context context,
            URI requestURI, REST.Request<UniNode> request,
            String[] urlParams
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: POST");
    }

    default REST.Response executePATCH(
            Context context,
            URI requestURI, REST.Request<UniNode> request,
            String[] urlParams
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: PATCH");
    }

    default REST.Response executeDELETE(
            Context context,
            URI requestURI, REST.Request<UniNode> request,
            String[] urlParams
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: DELETE");
    }

    default REST.Response executeHEAD(
            Context context,
            URI requestURI,
            REST.Request<UniNode> request, String[] urlParams
    ) throws RestEndpointException {
        throw new RestEndpointException(HTTPStatusCodes.METHOD_NOT_ALLOWED, "Method not supported: HEAD");
    }

    interface Underlying extends EndpointHandler {
        default EndpointHandler getEndpointHandler() {
            return this;
        }

        @Override
        default REST.Response executeMethod(Context context, URI requestURI, REST.Request<UniNode> request, String[] urlParams) throws RestEndpointException {
            return getEndpointHandler().executeMethod(context, requestURI, request, urlParams);
        }
    }
}
