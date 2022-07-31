package org.comroid.restless.body;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.WrappedFormattable;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public final class URIQueryEditor extends HashMap<String, Object> implements WrappedFormattable {
    private static final Logger logger = LogManager.getLogger();
    private final URI uri;

    @Override
    public String getPrimaryName() {
        return toString();
    }

    @Override
    public String getAlternateName() {
        return String.format("URIQueryEditor{uri=%s}", uri);
    }

    public URIQueryEditor(URI uri) {
        this.uri = uri;
        parseQuery(uri.getQuery(), this);
    }

    public static Map<String, Object> parseQuery(@Nullable String query) {
        return parseQuery(query, new HashMap<>());
    }

    @Contract("_, _ -> param2")
    public static Map<String, Object> parseQuery(@Nullable String query, Map<String, Object> yield) {
        if (query == null)
            return yield;

        // strip leading ? if present
        if (query.startsWith("?"))
            query = query.substring(1);

        try (
                Scanner scanner = new Scanner(query)
        ) {
            scanner.useDelimiter("&");

            while (scanner.hasNext()) {
                String[] pair = scanner.next().split("=");
                if (pair.length == 1)
                    logger.warn("Suspicious query parameter pair found: " + Arrays.toString(pair));
                yield.put(pair[0], pair.length == 1 ? null : pair[1]);
            }
        }
        return yield;
    }

    @Override
    public String toString() {
        logger.trace("Creating query string from editor:\n{}", entrySet().stream()
                .map(entry -> String.format("\t- %s => %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n")));
        return entrySet().stream()
                .map(entry -> String.format("%s=%s",
                                entry.getKey(), entry.getValue())
                        .replace(' ', '+'))
                .collect(Collectors.joining("&"));
    }

    public URI toURI() {
        try {
            URI uri = new URI(
                    this.uri.getScheme(),
                    this.uri.getAuthority(),
                    this.uri.getPath(),
                    toString(),
                    this.uri.getFragment()
            );
            logger.trace("Generated URI: {}", uri);
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid URI generated", e);
        }
    }

    public REST.Response toResponse(@MagicConstant(valuesFromClass = HTTPStatusCodes.class) int code) {
        return new REST.Response(code, toURI());
    }
}
