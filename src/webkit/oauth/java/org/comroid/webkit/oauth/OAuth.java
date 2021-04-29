package org.comroid.webkit.oauth;

import org.comroid.api.ContextualProvider;

public final class OAuth {
    public static String URL_BASE;
    public static ContextualProvider CONTEXT;

    private OAuth() {
        throw new UnsupportedOperationException();
    }

    public interface User {
        String getId();
    }

    public interface Service {
    }
}
