package org.comroid.restless.socket;

import org.comroid.api.Named;
import org.comroid.api.Rewrapper;

public interface WebsocketPacket {
    Type getType();

    default Rewrapper<Type> wrapType() {
        return this::getType;
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
}
