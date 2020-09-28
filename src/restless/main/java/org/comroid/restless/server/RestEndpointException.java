package org.comroid.restless.server;

import org.comroid.restless.HTTPStatusCodes;
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
}
