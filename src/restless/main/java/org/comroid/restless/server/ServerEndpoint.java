package org.comroid.restless.server;

import com.sun.net.httpserver.Headers;
import org.comroid.restless.REST;
import org.comroid.restless.endpoint.AccessibleEndpoint;
import org.comroid.uniform.node.UniNode;
import org.jetbrains.annotations.Contract;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public interface ServerEndpoint extends AccessibleEndpoint, EndpointHandler {
    AccessibleEndpoint getEndpointBase();

    default EndpointHandler getEndpointHandler() {
        return this;
    }

    @Override
    default Pattern getPattern() {
        return getEndpointBase().getPattern();
    }

    @Override
    default String getUrlBase() {
        return getEndpointBase().getUrlBase();
    }

    @Override
    default String getUrlExtension() {
        return getEndpointBase().getUrlExtension();
    }

    @Override
    default String[] getRegExpGroups() {
        return getEndpointBase().getRegExpGroups();
    }

    static ServerEndpoint combined(AccessibleEndpoint accessibleEndpoint, EndpointHandler handler) {
        return new Support.Combined(accessibleEndpoint, handler);
    }

    default boolean allowMemberAccess() {
        return false;
    }

    @Override
    default ServerEndpoint attachHandler(EndpointHandler handler) {
        throw new UnsupportedOperationException("Cannot attach Handler to ServerEndpoint");
    }

    default boolean isMemberAccess(String url) {
        return allowMemberAccess() && Stream.of(replacer(getRegExpGroups()), url)
                .mapToLong(str -> str.chars()
                        .filter(x -> x == '/')
                        .count())
                .distinct()
                .count() > 1;
    }

    @Override
    default boolean supports(REST.Method method) {
        return getEndpointHandler().supports(method);
    }

    @Override
    default REST.Response executeMethod(RestServer server, REST.Method method, Headers headers, String[] urlParams, String body) throws RestEndpointException {
        return getEndpointHandler().executeMethod(server, method, headers, urlParams, body);
    }

    final class Support {
        private static final class Combined implements ServerEndpoint {
            private final AccessibleEndpoint accessibleEndpoint;
            private final EndpointHandler handler;

            @Override
            public AccessibleEndpoint getEndpointBase() {
                return accessibleEndpoint;
            }

            @Override
            public EndpointHandler getEndpointHandler() {
                return handler;
            }

            public Combined(AccessibleEndpoint accessibleEndpoint, EndpointHandler handler) {
                this.accessibleEndpoint = accessibleEndpoint;
                this.handler = handler;
            }
        }
    }

    interface This extends ServerEndpoint {
        @Override
        String getUrlBase();

        @Override
        String getUrlExtension();

        @Override
        String[] getRegExpGroups();

        @Override
        Pattern getPattern();

        @Override
        @Contract("-> this")
        default AccessibleEndpoint getEndpointBase() {
            return this;
        }
    }
}
