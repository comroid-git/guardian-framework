package org.comroid.restless.endpoint;

import org.comroid.api.Polyfill;

import java.net.URI;
import java.net.URL;

public interface CompleteEndpoint {
    AccessibleEndpoint getEndpoint();

    String getSpec();

    default URL getURL() {
        return Polyfill.url(getSpec());
    }

    default URI getURI() {
        return Polyfill.uri(getSpec());
    }

    static CompleteEndpoint of(AccessibleEndpoint endpoint, String spec) {
        return new Support.OfSpec(endpoint, spec);
    }

    final class Support {
        private static final class OfSpec implements CompleteEndpoint {
            private final AccessibleEndpoint endpoint;
            private final String spec;

            @Override
            public AccessibleEndpoint getEndpoint() {
                return endpoint;
            }

            @Override
            public String getSpec() {
                return spec;
            }

            private OfSpec(AccessibleEndpoint endpoint, String spec) {
                this.endpoint = endpoint;
                this.spec = spec;
            }
        }
    }
}
