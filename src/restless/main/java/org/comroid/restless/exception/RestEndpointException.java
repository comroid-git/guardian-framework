package org.comroid.restless.exception;

import org.comroid.api.ContextualProvider;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.intellij.lang.annotations.MagicConstant;

public class RestEndpointException extends RuntimeException {
    private final int statusCode;
    private final String message;

    public int getStatusCode() {
        return statusCode;
    }

    public String getSimpleMessage() {
        return message;
    }

    public RestEndpointException(
            @MagicConstant(valuesFromClass = HTTPStatusCodes.class) int statusCode
    ) {
        this(statusCode, "No detail message");
    }

    public RestEndpointException(
            @MagicConstant(valuesFromClass = HTTPStatusCodes.class) int statusCode,
            String message
    ) {
        super(String.format("%s: %s", HTTPStatusCodes.toString(statusCode), message));

        this.statusCode = statusCode;
        this.message = message;
    }

    public RestEndpointException(
            @MagicConstant(valuesFromClass = HTTPStatusCodes.class) int statusCode,
            Throwable cause
    ) {
        this(statusCode, cause.getMessage(), cause);
    }

    public RestEndpointException(
            @MagicConstant(valuesFromClass = HTTPStatusCodes.class) int statusCode,
            String message,
            Throwable cause
    ) {
        super(String.format("%s: %s", HTTPStatusCodes.toString(statusCode), message), cause);

        this.statusCode = statusCode;
        this.message = message;
    }

    public interface RecoverStage {
        int DO_NOT_ATTEMPT = 0;
        int EXCEPTIONS_ONLY = 1;
        int ALL = 2;

        REST.Response tryRecover(
                ContextualProvider context,
                Throwable exception,
                String requestURI,
                int statusCode,
                REST.Method requestMethod,
                REST.Header.List requestHeaders
        );
    }
}
