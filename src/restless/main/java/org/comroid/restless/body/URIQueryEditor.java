package org.comroid.restless.body;

import java.net.URI;

public class URIQueryEditor {
    private final URI uri;

    public URIQueryEditor(URI uri) {
        this.uri = uri;
        this.query = uri.getQuery();
    }
}
