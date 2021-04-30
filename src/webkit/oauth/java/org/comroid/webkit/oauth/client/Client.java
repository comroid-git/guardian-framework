package org.comroid.webkit.oauth.client;

import org.comroid.api.Named;
import org.comroid.api.UUIDContainer;
import org.comroid.common.io.FileHandle;
import org.comroid.webkit.oauth.model.UserInfoProvider;
import org.comroid.webkit.oauth.resource.Resource;
import org.comroid.webkit.oauth.user.OAuthAuthorization;
import org.comroid.uniform.Context;

import java.util.Set;

public interface Client extends UUIDContainer, Named, UserInfoProvider {
    OAuthAuthorization createAuthorization(Context context, Resource resource, String userAgent, Set<String> scopes);

    String generateAuthorizationToken(Resource resource, String userAgent);

    boolean addAccessToken(OAuthAuthorization.AccessToken accessToken);

    FileHandle getDataDirectory();

    boolean checkScopes(Set<String> scopes);
}