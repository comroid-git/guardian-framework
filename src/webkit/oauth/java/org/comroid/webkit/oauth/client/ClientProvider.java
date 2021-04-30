package org.comroid.webkit.oauth.client;

import org.comroid.api.Rewrapper;
import org.comroid.restless.CommonHeaderNames;
import org.comroid.webkit.oauth.model.ValidityStage;
import org.comroid.webkit.oauth.user.OAuthAuthorization;
import org.comroid.restless.REST;
import org.comroid.restless.server.RestEndpointException;

import java.util.UUID;

public interface ClientProvider {
    OAuthAuthorization findAuthorization(String authorizationCode) throws RestEndpointException;

    default OAuthAuthorization.AccessToken findAccessToken(REST.Header.List headers) throws RestEndpointException {
        return findAccessToken(headers.getFirst(CommonHeaderNames.AUTHORIZATION));
    }

    OAuthAuthorization.AccessToken findAccessToken(String accessToken) throws RestEndpointException;

    boolean hasClient(UUID uuid);

    Rewrapper<? extends Client> findClient(UUID uuid);

    Client loginClient(String email, String login);

    ValidityStage findValidityStage(String token);
}
