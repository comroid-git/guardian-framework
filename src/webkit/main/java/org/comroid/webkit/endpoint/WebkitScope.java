package org.comroid.webkit.endpoint;

import org.comroid.restless.endpoint.EndpointScope;
import org.comroid.restless.server.EndpointHandler;
import org.intellij.lang.annotations.Language;

public enum WebkitScope implements EndpointScope, EndpointHandler {
    ;

    private final String extension;
    private final String[] regExp;

    @Override
    public String getUrlExtension() {
        return extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regExp;
    }

    WebkitScope(String extension, @Language("RegExp") String... regExp) {
        this.extension = extension;
        this.regExp = regExp;
    }
}
