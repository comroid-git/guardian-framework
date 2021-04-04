package org.comroid.webkit.frame;

import org.comroid.webkit.server.WebkitServer;
import org.jsoup.Jsoup;

import java.io.InputStream;

public final class FrameBuilder {
    public static ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    public FrameBuilder() {
        InputStream frame = classLoader.getResourceAsStream(WebkitServer.RESOURCE_PREFIX + "frame.html");
        Jsoup.
    }
}
