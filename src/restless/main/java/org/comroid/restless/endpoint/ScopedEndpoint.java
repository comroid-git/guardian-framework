package org.comroid.restless.endpoint;

import java.util.regex.Pattern;

public class ScopedEndpoint implements AccessibleEndpoint {
    private final EndpointScope scope;
    private final String urlBase;
    private final Pattern pattern;

    @Override
    public String getUrlBase() {
        return urlBase;
    }

    @Override
    public final String getUrlExtension() {
        return scope.getUrlExtension();
    }

    @Override
    public final String[] getRegExpGroups() {
        return scope.getRegExpGroups();
    }

    @Override
    public final Pattern getPattern() {
        return pattern;
    }

    public ScopedEndpoint(EndpointScope scope, String urlBase) {
        this.scope = scope;
        this.urlBase = urlBase;
        this.pattern = buildUrlPattern();
    }
}
