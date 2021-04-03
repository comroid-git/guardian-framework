package org.comroid.webkit.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.mutatio.model.RefPipe;
import org.comroid.mutatio.ref.ReferencePipe;
import org.comroid.util.Bitmask;
import org.comroid.webkit.socket.SocketFrame;
import org.graalvm.compiler.nodes.EncodedGraph;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebSocketConnection implements Closeable {
    private static final Logger logger = LogManager.getLogger();
    protected final RefPipe<Boolean, String, ?, ?> dataPipeline;
    private final Socket socket;
    private final Executor executor;
    private final ReaderThread reader;
    private final InputStream in;
    private final OutputStream out;

    public WebSocketConnection(Socket socket, Executor executor) throws IOException, NoSuchAlgorithmException {
        this.socket = socket;
        this.executor = executor;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
        this.reader = new ReaderThread();
        this.dataPipeline = new ReferencePipe<>();

        // read
        Scanner s = new Scanner(in, "UTF-8");
        String data = s.useDelimiter("\\r\\n\\r\\n").next();
        Matcher get = Pattern.compile("^GET").matcher(data);

        // handshake
        if (get.find()) {
            Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
            match.find();
            byte[] response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)))
                    + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            out.write(response, 0, response.length);

            // decode
            byte[] decoded = new byte[6];
            byte[] encoded = new byte[]{(byte) 198, (byte) 131, (byte) 130, (byte) 182, (byte) 194, (byte) 135};
            byte[] key = new byte[]{(byte) 167, (byte) 225, (byte) 225, (byte) 210};
            for (int i = 0; i < encoded.length; i++) {
                decoded[i] = (byte) (encoded[i] ^ key[i & 0x3]);
            }
        }

        executor.execute(reader);
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }

    public void sendText(final String payload) {
        try {
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
                    byte[] buffer = new byte[5120];
                    int read = in.read(buffer);
                    byte[] frame = Arrays.copyOf(buffer, read);
                    String data = new String(frame);

                    System.out.printf("Received Data Frame (len=%d;str='%s'):\n", read, data);
                    Bitmask.printByteArrayDump(frame);
                    System.out.println();
                    byte[] decoded = new byte[frame.length];
                    for (int i = 0; i < frame.length; i++) {
                        decoded[i] = frame[i] ^ mask[i % 4];
                    }

                    logger.debug("Data received: {}", data);
                    dataPipeline.accept(true, data);
                } catch (IOException e) {
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
