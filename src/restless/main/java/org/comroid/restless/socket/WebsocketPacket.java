package org.comroid.restless.socket;

import org.comroid.api.Named;
import org.comroid.api.Rewrapper;

public interface WebsocketPacket {
    Type getType();

    default Rewrapper<byte[]> getDataBytes() {
        return () -> getData().ifPresentMap(String::getBytes);
    }

    default Rewrapper<String> getData() {
        return Rewrapper.empty();
    }

    default Rewrapper<Integer> getStatusCode() {
        return Rewrapper.empty();
    }

    default Rewrapper<Throwable> getError() {
        return Rewrapper.empty();
    }

    static WebsocketPacket open() {
        class Open extends Empty {
            private Open() {
                super(Type.OPEN);
            }
        }

        return new Open();
    }

    static WebsocketPacket data(final String data) {
        class Text extends Empty {
            @Override
            public Rewrapper<String> getData() {
                return () -> data;
            }

            private Text() {
                super(Type.DATA);
            }
        }

        return new Text();
    }

    static WebsocketPacket data(final byte[] bytes) {
        class Bytes extends Empty {
            @Override
            public Rewrapper<byte[]> getDataBytes() {
                return () -> bytes;
            }

            @Override
            public Rewrapper<String> getData() {
                return () -> new String(bytes);
            }

            private Bytes() {
                super(Type.DATA);
            }
        }

        return new Bytes();
    }

    static WebsocketPacket error(final Throwable error) {
        class Error extends Empty {
            @Override
            public Rewrapper<Throwable> getError() {
                return () -> error;
            }

            private Error() {
                super(Type.ERROR);
            }
        }

        return new Error();
    }

    static WebsocketPacket close(final int code, final String reason) {
        class Closed extends Empty {
            @Override
            public Rewrapper<Integer> getStatusCode() {
                return () -> code;
            }

            @Override
            public Rewrapper<String> getData() {
                return () -> reason;
            }

            private Closed() {
                super(Type.CLOSE);
            }
        }

        return new Closed();
    }

    default Rewrapper<Type> wrapType() {
        return this::getType;
    }

    enum Type implements Named {
        OPEN,
        DATA,
        PING,
        PONG,
        CLOSE,
        ERROR;

        @Override
        public String getName() {
            return name();
        }
    }

    class Empty implements WebsocketPacket {
        private final Type type;

        @Override
        public Type getType() {
            return type;
        }

        public Empty(Type type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("WebSocketPacket<%s - %s>", type, shortDescription());
        }

        private String shortDescription() {
            switch (type) {
                case DATA:
                case PING:
                case PONG:
                    return getData().orElse("no data");
                case ERROR:
                    return getError().ifPresentMapOrElseGet(Throwable::toString, () -> "unknown error");
                case OPEN:
                    return "socket opened";
                case CLOSE:
                    return getData().ifPresentMap(str -> str + " #") + getStatusCode().orElse(-1);
            }
            return "unknown";
        }
    }
}
