package org.comroid.restless.endpoint;

import org.comroid.api.Polyfill;
import org.comroid.api.WrappedFormattable;
import org.comroid.common.ref.StaticCache;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public interface AccessibleEndpoint extends CompleteEndpoint, WrappedFormattable, Predicate<String>, EndpointScope {
    // fixme: add Url base whenever needed
    String getUrlBase();

    @Override
    default AccessibleEndpoint getEndpoint() {
        return this;
    }

    @Override
    default String getSpec() {
        int count = getParameterCount();
        if (count == 0)
            return getFullUrl();
        throw new IllegalStateException(String.format("Endpoint requires %d arguments!", count));
    }

    @Override
    default String getDefaultFormattedName() {
        return getFullUrl();
    }

    @Override
    default String getAlternateFormattedName() {
        return getUrlExtension();
    }

    /**
     * @return The complete, unformatted URL.
     */
    default String getFullUrl() {
        return getUrlBase() + getUrlExtension();
    }

    Pattern getPattern();

    @NonExtendable
    default CompleteEndpoint complete(Object... args) throws IllegalArgumentException {
        return CompleteEndpoint.of(this, string(args));
    }

    @NonExtendable
    default String string(Object... args) throws IllegalArgumentException {
        if (args.length != getParameterCount()) {
            throw new IllegalArgumentException("Invalid argument count");
        }

        String format = String.format(getUrlExtension(), args);

        if (test(format))
            return format;

        throw new IllegalArgumentException("Generated spec is invalid");
    }

    @NonExtendable
    default URL url(Object... args) throws IllegalArgumentException {
        return Polyfill.url(string(args));
    }

    @NonExtendable
    default URI uri(Object... args) throws IllegalArgumentException {
        return Polyfill.uri(string(args));
    }

    @NonExtendable
    default boolean test(URL url) {
        return test(url.toExternalForm());
    }

    @NonExtendable
    default boolean test(URI uri) {
        return test(uri.toString());
    }

    @Override
    @NonExtendable
    default boolean test(String url) {
        if (allowMemberAccess() && isMemberAccess(url)) {
            url = url.substring(0, url.lastIndexOf("/"));
        }

        final String[] regExpGroups = getRegExpGroups();
        String replacer = replacer(regExpGroups);
        Pattern pattern = getPattern();

        if (regExpGroups.length == 0)
            return pattern.matcher(url).matches() && replacer.equals(url);
        else {
            Matcher matcher = pattern.matcher(url);
            return matcher.matches() && matcher.replaceAll(replacer).equals(url);
        }
    }

    @NonExtendable
    default String[] extractArgs(URL url) {
        return extractArgs(url.toExternalForm());
    }

    @NonExtendable
    default String[] extractArgs(URI uri) {
        return extractArgs(uri.toString());
    }

    @NonExtendable
    default String[] extractArgs(String requestUrl) {
        if (allowMemberAccess() && isMemberAccess(requestUrl)) {
            requestUrl = requestUrl.substring(0, requestUrl.lastIndexOf("/"));
        }

        final Matcher matcher = getPattern().matcher(requestUrl);
        final String[] groups = getRegExpGroups();

        if (matcher.matches() && test(requestUrl)) {
            List<String> yields = new ArrayList<>();

            int i = 1;
            while (groups.length + 1 > i && matcher.matches())
                yields.add(matcher.group(i++));

            return yields.toArray(new String[0]);
        }

        return new String[0];
    }

    @Internal
    @NonExtendable
    default String replacer(String[] groups) {
        // todo: Inspect overhead
        return StaticCache.access(this, "replacer", () -> {
            String yield = getUrlExtension();

            int i = 0;
            while (yield.contains("%s") && groups.length > i) {
                int fi = yield.indexOf("%s");
                yield = String.format("%s$%d%s",
                        yield.substring(0, fi),
                        ++i,
                        yield.substring(fi + 2)
                );
            }

            return yield;
        });
    }

    @Internal
    @NonExtendable
    default Pattern buildUrlPattern() {
        final String[] regExpGroups = getRegExpGroups();

        if (regExpGroups != null && regExpGroups.length > 0)
            return Pattern.compile(String.format(getUrlExtension(), Arrays.stream(regExpGroups)
                    .map(str -> String.format("(%s)", str))
                    .toArray()));
        return Pattern.compile(getUrlExtension().replace("%s", "(.*)"));
    }

    default boolean allowMemberAccess() {
        return false;
    }

    default boolean isMemberAccess(String url) {
        return allowMemberAccess() && Stream.of(replacer(getRegExpGroups()), url)
                .mapToLong(str -> str.chars()
                        .filter(x -> x == '/')
                        .count())
                .distinct()
                .count() > 1;
    }
}
