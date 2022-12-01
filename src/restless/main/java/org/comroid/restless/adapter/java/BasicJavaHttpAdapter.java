package org.comroid.restless.adapter.java;

import org.comroid.api.Polyfill;
import org.comroid.restless.HttpAdapter;
import org.comroid.restless.REST;
import org.comroid.restless.socket.Websocket;
import org.jetbrains.annotations.ApiStatus.Experimental;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

@Experimental
public final class BasicJavaHttpAdapter implements HttpAdapter {
    @Override
    public CompletableFuture<? extends Websocket> createWebSocket(Executor executor, Consumer<Throwable> exceptionHandler, URI uri, REST.Header.List headers, String preferredProtocol) {
        throw new UnsupportedOperationException("Primitive Adapter Implementation; not capable of WebSockets");
    }

    @Override
    public CompletableFuture<REST.Response> call(REST.Request req) {
        final URL url = req.getEndpoint().getURL();

        try {
            // connect
            final HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod(req.getMethod().getName());
            req.getHeaders().forEach(con::setRequestProperty);
            con.setUseCaches(false);
            con.setDoOutput(true);

            // send
            final DataOutputStream dos = new DataOutputStream(con.getOutputStream());
            if (req.getBody() != null) {
                dos.writeBytes(req.getBody().toSerializedString());
            }
            dos.close();

            // receive
            InputStream is = con.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return null; //Todo
        } catch (IOException e) {
            return Polyfill.failedFuture(e);
        }
    }
}
