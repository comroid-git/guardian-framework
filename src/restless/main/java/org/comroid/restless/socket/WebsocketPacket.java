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
