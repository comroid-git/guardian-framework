package org.comroid.webkit.oauth.client;

import org.comroid.api.Named;
import org.comroid.api.UUIDContainer;
import org.comroid.common.io.FileHandle;
import org.comroid.uniform.Context;
import org.comroid.webkit.oauth.model.UserInfoProvider;
import org.comroid.webkit.oauth.resource.Resource;
import org.comroid.webkit.oauth.user.OAuthAuthorization;

import java.util.Set;

public interface Client extends UUIDContainer, Named, UserInfoProvider {
    FileHandle getDataDirectory();

    OAuthAuthorization createAuthorization(Context context, Resource resource, String userAgent, Set<String> scopes);

    String generateAuthorizationToken(Resource resource, String userAgent);

    boolean addAccessToken(OAuthAuthorization.AccessToken accessToken);

    boolean checkScopes(Set<String> scopes);
}
