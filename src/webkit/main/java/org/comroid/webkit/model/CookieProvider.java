package org.comroid.webkit.model;

import org.jetbrains.annotations.Nullable;

import java.time.Duration;

public interface CookieProvider {
    String getCookiePrefix();

    String getPlainCookie();

    @Nullable
    default Duration getDefaultCookieMaxAge() {
        return null;
    }

    @Nullable
    default String getDefaultCookiePath() {
        return "/";
    }

    @Nullable
    default String getDefaultCookieDomain() {
        return null;
    }

    default String getCookie() {
        return getCookie(getDefaultCookieMaxAge(), getDefaultCookiePath(), getDefaultCookieDomain());
    }

    static String assembleCookie(
            String prefix,
            String cookie,
            int maxAgeSeconds,
            @Nullable String path,
            @Nullable String domain
    ) {
        return assembleCookie(prefix, cookie, Duration.ofSeconds(maxAgeSeconds), path, domain);
    }

    static String assembleCookie(
            String prefix,
            String cookie
    ) {
        return assembleCookie(prefix, cookie, null, null, null);
    }

    static String assembleCookie(
            String prefix,
            String cookie,
            @Nullable Duration maxAge,
            @Nullable String path,
            @Nullable String domain
    ) {
        final StringBuilder sb = new StringBuilder(prefix + '=' + cookie);

        if (maxAge != null)
            sb.append("; Max-Age=").append(maxAge.getSeconds());
        if (path != null)
            sb.append("; Path=").append(path);
        if (domain != null)
            sb.append("; Domain=").append(domain);

        return sb.toString();
    }

    default String getCookie(@Nullable Duration maxAge, @Nullable String path, @Nullable String domain) {
        return assembleCookie(getCookiePrefix(), getPlainCookie(), maxAge, path, domain);
    }
}
