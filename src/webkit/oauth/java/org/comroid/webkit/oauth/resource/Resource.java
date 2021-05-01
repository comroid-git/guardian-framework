package org.comroid.webkit.oauth.resource;

import org.comroid.api.Named;
import org.comroid.api.UUIDContainer;
import org.comroid.webkit.oauth.user.OAuthAuthorization;

public interface Resource extends UUIDContainer, Named {
    String getSecret();

    String generateAccessToken(OAuthAuthorization authorization);
}
