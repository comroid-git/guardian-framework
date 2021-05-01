package org.comroid.webkit.oauth.resource;

import org.comroid.api.Named;
import org.comroid.api.UUIDContainer;
import org.comroid.util.Base64;
import org.comroid.webkit.oauth.user.OAuthAuthorization;

public interface Resource extends UUIDContainer, Named {
    String getSecret();

    default boolean checkBasicToken(String basicToken) {
        if (basicToken == null)
            return false;
        basicToken = Base64.decode(basicToken);
        if (!basicToken.startsWith("Basic "))
            return false;
        // strip prefix
        int index = basicToken.indexOf(' ');
        if (index < 8)
            basicToken = basicToken.substring(index + 1);
        String[] split = basicToken.split(":");
        return split[0].equals(getUUID().toString()) && split[1].equals(getSecret());
    }

    String generateAccessToken(OAuthAuthorization authorization);
}
