package org.comroid.webkit.socket;

import org.comroid.api.BitmaskEnum;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@SuppressWarnings("PointlessBitwiseExpression")
public final class SocketFrame {
    private final byte header;
    private final int maskingKey;
    private final long len;
    private final Reader payload;

    public byte[] getBytes() {
        int arrLen = 2;

        // find target array size
        boolean isMasked = Flag.MASK.isFlagSet(header);
        if (isMasked)
            arrLen += 4;
        boolean longPayload, longerPayload = false;
        if ((longPayload = (len >= 126)))
            arrLen += (longerPayload = (len > 0xFF)) ? 4 : 2;
        arrLen += len;
        byte header = (byte) (this.header | len >>> 9);

        // create byte array
        final byte[] bytes = new byte[arrLen];
        bytes[0] = header;

        if (longPayload) {
            // insert long payload length
            ByteBuffer byteBuffer = ByteBuffer.allocate(longerPayload ? 8 : 4);
            if (longerPayload)
                byteBuffer.putLong(len);
            else byteBuffer.putInt((int) len);
            byte[] lenBytes = byteBuffer.array();
            System.arraycopy(lenBytes, longerPayload ? 0 : 2, bytes, 2, longerPayload ? lenBytes.length : 2);
        }

        if (isMasked) {
            // insert masking key
            byte[] maskBytes = ByteBuffer.allocate(4)
                    .putInt(maskingKey)
                    .array();
            System.arraycopy(maskBytes, 0, bytes, longPayload ? longerPayload ? 10 : 4 : 2, 4);
        }

        // insert payload
        try {
            char[] payload = new char[(int) len];
            int read = this.payload.read(payload);
            if (read != len)
                throw new AssertionError("read was not equal to len");
            byte[] payloadBytes = new byte[payload.length];
            for (int i = 0; i < payload.length; i++)
                payloadBytes[i] = (byte) payload[i];
            System.arraycopy(payloadBytes, 0, bytes, 1 + (isMasked ? 4 : 0), payload.length);
        } catch (IOException e) {
            throw new RuntimeException("Could not read payload", e);
        }

        System.out.println("Generated Header:");
        for (int i = 0; i < bytes.length; i++) {
            byte each = bytes[i];
            String binaryString = Integer.toBinaryString(each);
            while (binaryString.length() < 8)
                binaryString = '0' + binaryString;
            System.out.printf("0x%2x [0b%s]\t", each, binaryString);
            if (i % 2 == 1)
                System.out.println();
        }

        return bytes;
    }

    private SocketFrame(byte header, int maskingKey, Reader payload, long len) {
        if (maskingKey > 0xFFFF)
            throw new IllegalArgumentException("Masking Key is too large");

        this.header = header;
        this.maskingKey = maskingKey;
        this.payload = payload;
        this.len = len;
    }

    public static SocketFrame create(
            String payload
    ) {
        return create(false, 0, payload);
    }

    public static SocketFrame create(
            boolean masked,
            int maskingKey,
            String payload
    ) {
        return create(false, false, false, masked, maskingKey, payload);
    }

    public static SocketFrame create(
            boolean rsv1,
            boolean rsv2,
            boolean rsv3,
            boolean masked,
            int maskingKey,
            String payload
    ) {
        return create(true, rsv1, rsv2, rsv3, Flag.TEXT, masked, maskingKey, new StringReader(payload), payload.length());
    }

    public static SocketFrame create(
            boolean fin,
            Reader payload,
            long length
    ) {
        return create(fin, false, (byte) 0, payload, length);
    }

    public static SocketFrame create(
            boolean fin,
            boolean masked,
            int maskingKey,
            Reader payload,
            long length
    ) {
        return create(fin, false, false, false, fin ? Flag.BYTES : Flag.CONTINUATION, masked, maskingKey, payload, length);
    }

    public static SocketFrame create(
            boolean fin,
            boolean rsv1,
            boolean rsv2,
            boolean rsv3,
            Flag opcode,
            boolean masked,
            int maskingKey,
            Reader payload,
            long length
    ) {
        int header;

        // expect 129 12
        int head1 = 0b1000_0001, headLen = 0b1000_1100, lenW125 = 0b0111_1101, lenW126 = 0b0111_1110, lenW127 = 0b0111_1111;

        header = opcode.getValue();
        header = header << 8;
        header = Flag.FIN.apply(header, fin);
        header = Flag.RSV_1.apply(header, rsv1);
        header = Flag.RSV_2.apply(header, rsv2);
        header = Flag.RSV_3.apply(header, rsv3);
        header = Flag.MASK.apply(header, masked);
        header = opcode.apply(header, true);

        header |= length >= 126 ? length > 126 ? 127 : 126 : length;

        return new SocketFrame((byte) header, maskingKey, payload, length);
    }

    public enum Flag implements BitmaskEnum<Flag> {
        @Internal
        FIN(0x8000),

        @Internal
        RSV_1(0x4000),
        @Internal
        RSV_2(0x2000),
        @Internal
        RSV_3(0x1000),

        @Internal
        MASK(0x0080),

        CONTINUATION(0x0000),
        TEXT(0x0100),
        BYTES(0x0200),
        ;

        private final int value;

        @Override
        public @NotNull Integer getValue() {
            return value;
        }

        Flag(int value) {
            this.value = value;
        }
    }
}
