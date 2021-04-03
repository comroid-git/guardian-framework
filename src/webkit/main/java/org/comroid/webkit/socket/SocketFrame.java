package org.comroid.webkit.socket;

import org.comroid.api.BitmaskEnum;
import org.comroid.restless.socket.Websocket;
import org.comroid.restless.socket.WebsocketPacket;
import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;

import static org.comroid.util.Bitmask.*;

public final class SocketFrame {
    private SocketFrame() {
        throw new UnsupportedOperationException();
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
        System.out.printf("OPCode Flagged (%1b;%1b;%1b;%1b): ", fin, rsv1, rsv2, rsv3);
        printByteDump(headerA);
        System.out.println();

        headerA |= (byte) opCode;
        System.out.print("OP Code + Header: ");
        printIntegerBytes(opCode);
        printByteDump(headerA);
        System.out.println();

        byte headerB = (byte) (len >= 126 ? len > 126 ? 127 : 126 : len);
        System.out.printf("String Len (%d): ", len);
        printByteDump(headerB);
        System.out.println();
        if (masked)
            headerB |= 1 << 7;
        System.out.print("With Mask: ");
        printByteDump(headerB);
        System.out.println();

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

        System.out.println("Generated Headers:");
        printByteArrayDump(bytes);

        if (longPayload) {
            // insert long payload length
            ByteBuffer byteBuffer = ByteBuffer.allocate(longerPayload ? 8 : 4);
            if (longerPayload)
                byteBuffer.putLong(len);
            else byteBuffer.putInt((int) len);
            byte[] lenBytes = byteBuffer.array();
            System.arraycopy(lenBytes, longerPayload ? 0 : 2, bytes, 2, longerPayload ? lenBytes.length : 2);
        }
        System.out.printf("With Lengths (%d;%b;%b):\n", len, longPayload, longerPayload);
        printByteArrayDump(bytes);

        if (masked) {
            // insert masking key
            byte[] maskBytes = ByteBuffer.allocate(4)
                    .putInt(maskingKey)
                    .array();
            System.arraycopy(maskBytes, 0, bytes, longPayload ? longerPayload ? 10 : 4 : 2, 4);
        }
        System.out.printf("With Mask Key (%d):", maskingKey);
        printIntegerBytes(maskingKey);
        System.out.println("Bytes Now:");
        printByteArrayDump(bytes);

        // insert payload
        try {
            char[] payloadChars = new char[(int) len];
            int read = payload.read(payloadChars);
            if (read != len)
                throw new AssertionError("read was not equal to len");
            byte[] payloadBytes = new byte[payloadChars.length];
            for (int i = 0; i < payloadChars.length; i++)
                payloadBytes[i] = (byte) payloadChars[i];
            int destPos = 2 + (masked ? 4 : 0) + (longPayload ? longerPayload ? 8 : 2 : 0);
            System.out.printf("Payload Bytes (into %d):\n", destPos);
            printByteArrayDump(payloadBytes);
            System.arraycopy(payloadBytes, 0, bytes, destPos, payloadChars.length);
        } catch (IOException e) {
            throw new RuntimeException("Could not read payload", e);
        }

        System.out.println("Generated Frame:");
        printByteArrayDump(bytes);

        return bytes;
    }

    public static class OpCode {
        public static final int CONTINUATION = 0x0;
        public static final int TEXT = 0x1;
        public static final int BINARY = 0x2;
    }
}
