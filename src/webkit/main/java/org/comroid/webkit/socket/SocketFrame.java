package org.comroid.webkit.socket;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.MagicConstant;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.comroid.util.Bitmask.*;

public final class SocketFrame {
    private static final Logger logger = LogManager.getLogger();
    private final boolean last;
    private final boolean rsv1;
    private final boolean rsv2;
    private final boolean rsv3;
    private final boolean isMasked;
    private final int opCode;
    private final byte[] mask;
    private final byte[] encoded;
    private final long len;

    public boolean isLast() {
        return last;
    }

    public boolean isRsv1() {
        return rsv1;
    }

    public boolean isRsv2() {
        return rsv2;
    }

    public boolean isRsv3() {
        return rsv3;
    }

    public boolean isMasked() {
        return isMasked;
    }

    private int getOpCode() {
        return opCode;
    }

    private SocketFrame(
            boolean last,
            boolean rsv1,
            boolean rsv2,
            boolean rsv3,
            boolean isMasked,
            int opCode, byte[] mask,
            byte[] encoded,
            long len
    ) {
        this.last = last;
        this.rsv1 = rsv1;
        this.rsv2 = rsv2;
        this.rsv3 = rsv3;
        this.isMasked = isMasked;
        this.opCode = opCode;
        this.mask = mask;
        this.encoded = encoded;
        this.len = len;
    }

    public static byte[] create(
            String payload
    ) {
        return create(false, 0, payload);
    }

    public static byte[] create(
            boolean masked,
            int maskingKey,
            String payload
    ) {
        return create(false, false, false, masked, maskingKey, payload);
    }

    public static byte[] create(
            boolean rsv1,
            boolean rsv2,
            boolean rsv3,
            boolean masked,
            int maskingKey,
            String payload
    ) {
        return create(true, rsv1, rsv2, rsv3, OpCode.TEXT, masked, maskingKey, new StringReader(payload), payload.length());
    }

    public static byte[] create(
            boolean fin,
            @MagicConstant(valuesFromClass = SocketFrame.OpCode.class) int opCode,
            Reader payload,
            long length
    ) {
        return create(fin, opCode, false, (byte) 0, payload, length);
    }

    public static byte[] create(
            boolean fin,
            @MagicConstant(valuesFromClass = SocketFrame.OpCode.class) int opCode,
            boolean masked,
            int maskingKey,
            Reader payload,
            long length
    ) {
        return create(fin, false, false, false, opCode, masked, maskingKey, payload, length);
    }

    public static byte[] create(
            boolean fin,
            boolean rsv1,
            boolean rsv2,
            boolean rsv3,
            @MagicConstant(valuesFromClass = SocketFrame.OpCode.class) int opCode,
            boolean masked,
            int maskingKey,
            Reader payload,
            long len
    ) {
        if (maskingKey > 0xFFFF)
            throw new IllegalArgumentException("Masking Key is too large");

        byte headerA = 0;

        if (fin)
            headerA |= 1 << 7;
        if (rsv1)
            headerA |= 1 << 6;
        if (rsv2)
            headerA |= 1 << 5;
        if (rsv3)
            headerA |= 1 << 4;

        logger.trace(createByteDump(String.format("OPCode Flagged (%1b;%1b;%1b;%1b): ", fin, rsv1, rsv2, rsv3), headerA));

        headerA |= (byte) opCode;
        printIntegerBytes(logger, "OP Code + Header: ", opCode);
        logger.trace(createByteDump("Header A", headerA));

        byte headerB = (byte) (len >= 126 ? len > 126 ? 127 : 126 : len);
        logger.trace(createByteDump("Header B with len", headerB));
        if (masked)
            headerB |= 1 << 7;
        logger.trace(createByteDump("Header B with Mask", headerB));

        headerA = (byte) 0b1000_0001;
        headerB = (byte) 0b0000_1100;

        int arrLen = 2;

        // find target array size
        if (masked)
            arrLen += 4;
        boolean longPayload, longerPayload = false;
        if ((longPayload = (len >= 126)))
            arrLen += (longerPayload = (len > 0xFF)) ? 4 : 2;
        arrLen += len;

        // create byte array
        final byte[] bytes = new byte[arrLen];
        bytes[0] = headerA;
        bytes[1] = headerB;

        printByteArrayDump(logger, "Generated Headers:", bytes);

        if (longPayload) {
            // insert long payload length
            ByteBuffer byteBuffer = ByteBuffer.allocate(longerPayload ? 8 : 4);
            if (longerPayload)
                byteBuffer.putLong(len);
            else byteBuffer.putInt((int) len);
            byte[] lenBytes = byteBuffer.array();
            System.arraycopy(lenBytes, longerPayload ? 0 : 2, bytes, 2, longerPayload ? lenBytes.length : 2);
        }

        printByteArrayDump(logger, String.format("With Lengths (%d;%b;%b):", len, longPayload, longerPayload), bytes);

        if (masked) {
            // insert masking key
            byte[] maskBytes = ByteBuffer.allocate(4)
                    .putInt(maskingKey)
                    .array();
            System.arraycopy(maskBytes, 0, bytes, longPayload ? longerPayload ? 10 : 4 : 2, 4);
        }

        printIntegerBytes(logger, String.format("With Mask Key (%d):", maskingKey), maskingKey);
        printByteArrayDump(logger, "Bytes Now:", bytes);

        // insert payload
        byte[] payloadBytes;
        try {
            char[] payloadChars = new char[(int) len];
            int read = payload.read(payloadChars);
            if (read != len)
                throw new AssertionError("read was not equal to len");
            payloadBytes = new byte[payloadChars.length];
            for (int i = 0; i < payloadChars.length; i++)
                payloadBytes[i] = (byte) payloadChars[i];
            int destPos = 2 + (masked ? 4 : 0) + (longPayload ? longerPayload ? 8 : 2 : 0);

            printByteArrayDump(logger, String.format("Payload Bytes (into %d):", destPos), payloadBytes);
            System.arraycopy(payloadBytes, 0, bytes, destPos, payloadChars.length);
        } catch (IOException e) {
            throw new RuntimeException("Could not read payload", e);
        }

        printByteArrayDump(logger, "Generated Frame:", bytes);

        // validate frame
        InputStream validator = new InputStream() {
            private int index;

            @Override
            public int read() {
                return bytes[index++];
            }
        };
        SocketFrame frame = readFrame(validator);
        if (frame.isLast() != fin)
            generatedFrameInvalid("frame", "last");
        if (frame.isRsv1() != rsv1)
            generatedFrameInvalid("rsv1", rsv1);
        if (frame.isRsv2() != rsv2)
            generatedFrameInvalid("rsv2", rsv2);
        if (frame.isRsv3() != rsv3)
            generatedFrameInvalid("rsv3", rsv3);
        //noinspection MagicConstant
        if (frame.getOpCode() != opCode)
            generatedFrameInvalid("opCode", 'x' + Integer.toHexString(opCode));
        if (frame.isMasked() != masked)
            generatedFrameInvalid("masked", masked);
        if (frame.length() != len)
            generatedFrameInvalid("length", len);
        if (!Arrays.equals(frame.decodeData(), payloadBytes))
            generatedFrameInvalid("payload", "equals");

        return bytes;
    }

    private static void generatedFrameInvalid(String what, Object expect) throws AssertionError {
        throw new AssertionError(String.format("Generated Frame is invalid: %s is not %s", what, expect));
    }

    public static SocketFrame readFrame(InputStream in) {
        try {
            byte headerA = (byte) in.read();
            boolean isLast = (headerA & 1 << 7) != 0;
            boolean rsv1 = (headerA & 1 << 6) != 0;
            boolean rsv2 = (headerA & 1 << 6) != 0;
            boolean rsv3 = (headerA & 1 << 6) != 0;
            int opCode = (headerA & 0b0000_1111);
            logger.trace("last = {}; rsv1 = {}; rsv2 = {}; rsv3 = {}; op = x{}", isLast, rsv1, rsv2, rsv3, Integer.toHexString(opCode));
            logger.trace(createByteDump("Header A", headerA));

            byte headerB = (byte) in.read();
            boolean isMasked = (headerB & 1 << 7) != 0;

            int payLen = (headerB & 0b0111_1111);
            long len;
            switch (payLen) {
                default:
                    len = payLen;
                    break;
                case 126:
                    len = readNextN(in, 2).getLong();
                    break;
                case 127:
                    len = readNextN(in, 8).getLong();
                    break;
            }

            logger.trace(createByteDump(String.format("isMasked = %b; payLen = %d; len = %d", isMasked, payLen, len), headerB));

            byte[] mask = isMasked ? readNextN(in, 4).array() : new byte[0];

            byte[] encoded = readNextN(in, (int) len).array();

            return new SocketFrame(isLast, rsv1, rsv2, rsv3, isMasked, opCode, mask, encoded, len);
        } catch (Throwable t) {
            throw new RuntimeException("Could not read frame from " + in, t);
        }
    }

    private static ByteBuffer readNextN(InputStream in, int lenLen) throws IOException {
        byte[] lenBytesB = new byte[lenLen];
        int readB = in.read(lenBytesB);
        if (readB != lenBytesB.length)
            throw new IllegalStateException("Could not read desired length");
        return ByteBuffer.wrap(lenBytesB);
    }

    public long length() {
        return len;
    }

    public byte[] decodeData() {
        if (!isMasked)
            return encoded;
        byte[] decoded = new byte[encoded.length];
        for (int i = 0; i < encoded.length; i++) {
            decoded[i] = (byte) (encoded[i] ^ mask[i % 4]);
        }
        return decoded;
    }

    public static class OpCode {
        public static final int CONTINUATION = 0x0;
        public static final int TEXT = 0x1;
        public static final int BINARY = 0x2;
    }
}
