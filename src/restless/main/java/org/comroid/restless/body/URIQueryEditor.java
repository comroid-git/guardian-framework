package org.comroid.restless.body;

import org.comroid.api.WrappedFormattable;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.util.StandardValueType;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public final class URIQueryEditor extends HashMap<String, Object> implements WrappedFormattable {
    private final URI uri;

    @Override
    public String getDefaultFormattedName() {
        return toString();
    }

    @Override
    public String getAlternateFormattedName() {
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
                yield.put(pair[0], StandardValueType.findGoodType(pair[1]));
            }
        }
        return yield;
    }

    @Override
    public String toString() {
        return entrySet().stream()
                .map(entry -> String.format("%s=%s",
                        entry.getKey().replace(' ', '+'),
                        String.valueOf(entry.getValue()).replace(' ', '+')))
                .collect(Collectors.joining("&"));
    }

    public URI toURI() {
        try {
            return new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    toString(),
                    uri.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Invalid URI generated", e);
        }
    }

    public REST.Response toResponse(@MagicConstant(valuesFromClass = HTTPStatusCodes.class) int code) {
        return new REST.Response(code, toURI());
    }
}
