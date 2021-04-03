package org.comroid.restless.endpoint;

import org.comroid.mutatio.ref.Reference;

import java.util.function.Supplier;
import java.util.regex.Pattern;

public class ScopedEndpoint implements AccessibleEndpoint {
    private final EndpointScope scope;
    private final Reference<String> urlBase;
    private final Pattern pattern;

    @Override
    public final String getUrlBase() {
        return urlBase.assertion("URL Base not found");
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

    public ScopedEndpoint(EndpointScope scope, Supplier<String> urlBaseSupplier) {
        this.scope = scope;
        this.urlBase = Reference.provided(urlBaseSupplier);
        this.pattern = buildUrlPattern();
    }
}
