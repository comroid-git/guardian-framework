package org.comroid.restless.socket;

import org.comroid.api.StringSerializable;
import org.comroid.api.UncheckedCloseable;

public final class WebSocketClientSpec {
    private WebSocketClientSpec() {
        throw new UnsupportedOperationException();
    }

    public interface Closeable extends UncheckedCloseable {
        boolean isOpen();

        void close(int code, String reason);

        default void close(int code) {
            close(code, "Connection Closed");
        }

        @Override
        default void close() {
            close(1000);
        }
    }

    public interface Sender {
        default void sendText(StringSerializable serializable) {
            sendText(serializable.toSerializedString());
        }

        void sendText(String text);

        void sendBinary(byte[] bytes);

        void sendPing();
    }

    public interface Complete extends Closeable, Sender {
    }
}
