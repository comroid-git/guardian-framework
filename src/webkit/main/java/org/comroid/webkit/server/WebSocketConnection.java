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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Scanner;
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
        Scanner handshakeReader = new Scanner(in).useDelimiter("\\r\\n\\r\\n");
        String requestString = handshakeReader.next();
        //System.out.println("requestString = " + requestString);

        if (!requestString.startsWith("GET"))
            throw new IllegalArgumentException("Illegal request method: " + requestString.substring(requestString.indexOf(' ')));

        this.headers = new REST.Header.List();
        String[] lines = requestString.split("\n");
        Pattern headerPattern = Pattern.compile("/([\\w-]+): (.*)/g");
        for (int i = 1; i < lines.length; i++) {
            Matcher matcher = headerPattern.matcher(lines[i]);

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

        String httpString = new REST.Response(HTTPStatusCodes.SWITCH_PROTOCOL, responseHeaders).toHttpString();
        logger.trace("Handshaking with HTTP Response:\n{}", httpString);
        out.write(httpString.getBytes(StandardCharsets.UTF_8));
        out.flush();

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
        logger.debug("Sending Text frame with payload: {}", payload);
        sendFrameData(SocketFrame.create(payload));
    }

    public void sendPing() {
        logger.debug("Sending Ping");
        sendFrameData(SocketFrame.create(true, SocketFrame.OpCode.PING));
    }

    public void sendPong(byte[] pingData) {
        logger.debug("Sending Pong");
        sendFrameData(SocketFrame.create(true, SocketFrame.OpCode.PONG, pingData));
    }

    private void sendFrameData(byte[] frameData) {
        try {
            out.write(frameData);
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

                        switch (frame.getOpCode()) {
                            case SocketFrame.OpCode.CLOSE:
                                logger.debug("Connection closed by Socket");
                                close();
                                break;
                            case SocketFrame.OpCode.PING:
                                sendPong(frame.decodeData());
                                break;
                            default:
                                isLast = frame.isLast();

                                byte[] payload = frame.decodeData();
                                String str = new String(payload, StandardCharsets.UTF_8);

                                Bitmask.printByteArrayDump(logger, String.format("Received Payload (len=%d;str='%s'):\n", frame.length(), str), payload);

                                data.append(str);
                                break;
                        }
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
