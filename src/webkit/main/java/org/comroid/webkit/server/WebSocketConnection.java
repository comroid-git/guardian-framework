package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.StringSerializable;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.model.RefPipe;
import org.comroid.mutatio.ref.ReferencePipe;
import org.comroid.restless.HTTPStatusCodes;
import org.comroid.restless.REST;
import org.comroid.util.Bitmask;
import org.comroid.webkit.socket.SocketFrame;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketConnection implements Closeable {
    private static final Logger logger = LogManager.getLogger();
    protected final RefPipe<?, String, ?, String> dataPipeline;
    private final Socket socket;
    private final Executor executor;
    private final ReaderThread reader;
    private final InputStream in;
    private final OutputStream out;
    private final REST.Header.List headers;

    public final RefContainer<?, String> getDataPipeline() {
        return dataPipeline;
    }

    public REST.Header.List getHeaders() {
        return headers;
    }

    public WebSocketConnection(Socket socket, Executor executor) throws IOException, NoSuchAlgorithmException {
        this.socket = socket;
        this.executor = executor;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.reader = new ReaderThread();
        this.dataPipeline = new ReferencePipe<>(executor);

        // read
        InputStreamReader isr = new InputStreamReader(in);
        BufferedReader handshakeReader = new BufferedReader(isr);
        String requestHead = handshakeReader.readLine();

        this.headers = new REST.Header.List();
        String headerLine;
        Pattern headerPattern = Pattern.compile("/([\\w-]+): (.*)/g");
        while (true) {
            headerLine = handshakeReader.readLine();
            Matcher matcher = headerPattern.matcher(headerLine);

            if (!matcher.matches())
                break;
            String headerName = matcher.group(1);
            String headerValues = matcher.group(2);

            headers.add(headerName, headerValues.split("; "));
        }

        REST.Header.List responseHeaders = new REST.Header.List();

        String websocketKey = headers.getFirst("Sec-WebSocket-Key");
        responseHeaders.add("Connection", "Upgrade");
        responseHeaders.add("Upgrade", "websocket");
        responseHeaders.add("Sec-WebSocket-Accept", encodeSocketKey(websocketKey));

        Reader reader = new REST.Response(HTTPStatusCodes.SWITCH_PROTOCOL, responseHeaders).toReader();
        OutputStreamWriter osw = new OutputStreamWriter(out);
        char[] buf = new char[512];
        int r, c = 0;
        while ((r = reader.read(buf)) >= 512) {
            System.out.println("Writing response part:\n" + new String(buf));
            osw.write(buf, c, r);
            c += r;
        }
        System.out.println("Writing response end:\n" + new String(Arrays.copyOf(buf, r)));
        osw.write(buf, c, r);
        osw.flush();

        executor.execute(this.reader);
    }

    public static String encodeSocketKey(String key) throws NoSuchAlgorithmException {
        return Base64.getEncoder()
                .encodeToString(MessageDigest.getInstance("SHA-1")
                        .digest((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                                .getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public void sendText(StringSerializable serializable) {
        sendText(serializable.toSerializedString());
    }

    public void sendText(final String payload) {
        try {
            logger.debug("Sending Text frame with payload: {}", payload);
            byte[] frame = SocketFrame.create(payload);
            out.write(frame);
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException("Could not send data frame", e);
        }
    }

    private final class ReaderThread implements Runnable {
        @Override
        public void run() {
            logger.debug("Starting Reader");
            while (true) {
                try {
                    boolean isLast = false;
                    StringBuilder data = new StringBuilder();

                    while (!isLast) {
                        SocketFrame frame = SocketFrame.readFrame(in);
                        isLast = frame.isLast();

                        byte[] payload = frame.decodeData();
                        String str = new String(payload, StandardCharsets.UTF_8);

                        Bitmask.printByteArrayDump(logger, String.format("Received Payload (len=%d;str='%s'):\n", frame.length(), str), payload);

                        data.append(str);
                    }

                    logger.debug("Server received: {}", data.toString());
                    dataPipeline.accept(null, data.toString());
                } catch (Throwable e) {
                    logger.fatal("Error ocurred in connection reader; closing connection", e);
                    try {
                        socket.close();
                    } catch (IOException e2) {
                        logger.fatal("Could not close connection", e2);
                    }
                    return;
                }
            }
        }
    }
}
