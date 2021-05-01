package org.comroid.webkit.oauth.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.StreamSupplier;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.restless.body.URIQueryEditor;
import org.comroid.restless.server.RestEndpointException;
import org.comroid.restless.server.ServerEndpoint;
import org.comroid.uniform.Context;
import org.comroid.uniform.node.UniNode;
import org.comroid.util.Pair;
import org.comroid.webkit.frame.FrameBuilder;
import org.comroid.webkit.model.PagePropertiesProvider;
import org.comroid.webkit.oauth.OAuth;
import org.comroid.webkit.oauth.client.Client;
import org.comroid.webkit.oauth.client.ClientProvider;
import org.comroid.webkit.oauth.model.OAuthError;
import org.comroid.webkit.oauth.model.ValidityStage;
import org.comroid.webkit.oauth.resource.Resource;
import org.comroid.webkit.oauth.resource.ResourceProvider;
import org.comroid.webkit.oauth.rest.request.AuthenticationRequest;
import org.comroid.webkit.oauth.rest.request.TokenRequest;
import org.comroid.webkit.oauth.rest.request.TokenRevocationRequest;
import org.comroid.webkit.oauth.user.OAuthAuthorization;
import org.intellij.lang.annotations.Language;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.comroid.restless.HTTPStatusCodes.*;

// fixme fixme fixme
public enum OAuthEndpoint implements ServerEndpoint.This {
    AUTHORIZE("/authorize") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            AuthenticationRequest authenticationRequest = new AuthenticationRequest(context, body.asObjectNode());
            URI redirectURI = authenticationRequest.getRedirectURI();
            URIQueryEditor query = new URIQueryEditor(redirectURI);
            logger.debug("Got {}", authenticationRequest);

            // find service by client id
            final ResourceProvider resourceProvider = context.requireFromContext(ResourceProvider.class);
            final UUID clientID = authenticationRequest.getClientID();

            if (!resourceProvider.hasResource(clientID)) {
                query.put("error", OAuthError.INVALID_REQUEST);
                return query.toResponse(FOUND);
            }

            final Resource service = resourceProvider.getResource(clientID)
                    .orElseThrow(() -> new RestEndpointException(UNAUTHORIZED, "Resource with ID " + clientID + " not found"));
            final String userAgent = headers.getFirst(CommonHeaderNames.USER_AGENT);

            try {
                // find client
                Client client = context.requireFromContext(ClientProvider.class)
                        .findClient(headers)
                        // throw with status code OK to send login frame
                        .orElseThrow(() -> new RestEndpointException(OK));

                String authorizationCode = completeAuthorization(client, authenticationRequest, context, service, userAgent);

                // assemble redirect uri
                query.put("code", authorizationCode);
                if (authenticationRequest.state.isNonNull())
                    query.put("state", authenticationRequest.getState());
            } catch (RestEndpointException e) {
                if (e.getStatusCode() != UNAUTHORIZED)
                    query.put("error", OAuthError.SERVER_ERROR.getValue());
                else {
                    // send frame and obtain session from there
                    Map<String, Object> pageProps = context.requireFromContext(PagePropertiesProvider.class)
                            .findPageProperties(headers);
                    FrameBuilder frame = new FrameBuilder("quickAction", headers, pageProps, false);
                    frame.setPanel("flowLogin");

                    UUID requestId = UUID.randomUUID();
                    loginRequests.put(requestId, authenticationRequest);
                    Map<String, Object> flow = new HashMap<>();
                    flow.put("requestId", requestId);
                    flow.put("resourceName", service.getName());
                    flow.put("scopes", authenticationRequest.getScopes());
                    frame.setProperty("flow", flow);

                    return new REST.Response(OK, "text/html", frame.toReader());
                }
            } catch (Exception e) {
                logger.warn("Could not authorize OAuth session; aborting", e);

                // fixme use correct codes
                query.put("error", OAuthError.SERVER_ERROR.getValue());
            }

            return new REST.Response(HTTPStatusCodes.FOUND, query.toURI());
        }
    },
    AUTHORIZE_LOGIN("/authorize/login") {
        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            String email = body.get("email").asString(str -> str.replace("%40", "@"));
            String login = body.get("password").asString();

            UUID requestId = body.get("requestId").asString(UUID::fromString);
            AuthenticationRequest authenticationRequest = loginRequests.getOrDefault(requestId, null);
            if (authenticationRequest == null)
                throw new RestEndpointException(BAD_REQUEST, "Invalid request ID: " + requestId);
            URIQueryEditor query = new URIQueryEditor(authenticationRequest.getRedirectURI());

            Pair<Client, String> client;
            try {
                client = context.requireFromContext(ClientProvider.class)
                        .loginClient(email, login);
            } catch (RestEndpointException e) {
                query.put("error", OAuthError.UNAUTHORIZED_CLIENT.getValue());
                return new REST.Response(FOUND, query.toURI());
            }

            UUID clientID = authenticationRequest.getClientID();
            Resource service = context.requireFromContext(ResourceProvider.class)
                    .getResource(clientID)
                    .orElseThrow(() -> new RestEndpointException(UNAUTHORIZED, "Service with ID " + clientID + " not found"));
            String userAgent = headers.getFirst(CommonHeaderNames.USER_AGENT);

            String code = OAuthEndpoint.completeAuthorization(client.getFirst(), authenticationRequest, context, service, userAgent);

            // assemble redirect uri
            query.put("code", code);
            if (authenticationRequest.state.isNonNull())
                query.put("state", authenticationRequest.getState());

            REST.Header.List response = new REST.Header.List();
            response.add("Location", query.toURI().toString());
            response.add("Set-Cookie", client.getSecond());

            return new REST.Response(FOUND, response);
        }
    },
    TOKEN("/token") {
        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            TokenRequest.AuthorizationCodeGrant tokenRequest = new TokenRequest.AuthorizationCodeGrant(context, body.asObjectNode());
            OAuthAuthorization authorization = context.requireFromContext(ClientProvider.class)
                    .findAuthorization(tokenRequest.getCode());
            OAuthAuthorization.AccessToken accessToken = authorization.createAccessToken();

            return new REST.Response(OK, accessToken);
        }
    },
    TOKEN_REVOKE("/token/revoke") {
        @Override
        public REST.Response executePOST(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            ClientProvider clientProvider = context.requireFromContext(ClientProvider.class);
            TokenRevocationRequest request = new TokenRevocationRequest(context, body);

            ValidityStage validity;
            if (request.tokenHint.isNull()) {
                validity = clientProvider.findValidityStage(request.getToken());
            } else switch (request.getTokenHint()) {
                case "access_token":
                    validity = clientProvider.findAccessToken(request.getToken());
                    break;
                case "refresh_token":
                    // fixme
                    //validity = clientProvider.findAccessToken(request.getToken());
                    throw new UnsupportedOperationException("unsupported: refresh token");
                default:
                    throw new AssertionError("invalid token hint: " + request.getTokenHint());
            }

            if (validity == null)
                throw new RestEndpointException(BAD_REQUEST, "Unknown Token");
            if (validity.isValid() && !validity.invalidate())
                throw new RestEndpointException(INTERNAL_SERVER_ERROR, "Could not invalidate token");
            return new REST.Response(OK);
        }
    },
    USER_INFO("/userInfo") {
        @Override
        public REST.Response executeGET(Context context, REST.Header.List headers, String[] urlParams, UniNode body) throws RestEndpointException {
            UniNode accountData = context.requireFromContext(ClientProvider.class)
                    .findAccessToken(headers)
                    .getAuthorization()
                    .getUserInfo();
            return new REST.Response(OK, accountData);
        }

        @Override
        public boolean allowMemberAccess() {
            return true;
        }
    };

    public static final StreamSupplier<ServerEndpoint> values = StreamSupplier.of(values());
    private static final Map<UUID, AuthenticationRequest> loginRequests = new ConcurrentHashMap<>();
    private static final Logger logger = LogManager.getLogger();
    private final String extension;
    private final String[] regExp;
    private final Pattern pattern;

    @Override
    public String getUrlBase() {
        return OAuth.URL_BASE;
    }

    @Override
    public String getUrlExtension() {
        return "/oauth2" + extension;
    }

    @Override
    public String[] getRegExpGroups() {
        return regExp;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    OAuthEndpoint(String extension, @Language("RegExp") String... regExp) {
        this.extension = extension;
        this.regExp = regExp;
        this.pattern = buildUrlPattern();
    }

    private static String completeAuthorization(Client client, AuthenticationRequest request, Context context, Resource resource, String userAgent) {
        Set<String> scopes = request.getScopes();

        // fixme
        // validate account has scopes as permit
        if (!client.checkScopes(scopes))
            throw new RestEndpointException(UNAUTHORIZED, "Scope check invalid");

        // create oauth blob for user with this service + user agent
        OAuthAuthorization authorization = client.createAuthorization(context, resource, userAgent, scopes);
        return authorization.getCode();
    }
}
