package org.comroid.mutatio.cache;

import java.io.Closeable;
import java.util.function.Consumer;

public interface ValueUpdateListener<T> extends Closeable {
    static <T> ValueUpdateListener<T> ofConsumer(SingleValueCache<T> parent, Consumer<T> consumer) {
        return new Support.OfConsumer<>(parent, consumer);
    }

    void acceptNewValue(T value);

    @Override
    void close();

    final class Support {
        public static abstract class Base<T> implements ValueUpdateListener<T> {
            private final SingleValueCache<T> parent;

            public Base(SingleValueCache<T> parent) {
                this.parent = parent;

                parent.attach(this);
            }

            @Override
            public void close() {
                parent.detach(this);
            }
        }

        private static final class OfConsumer<T> extends Base<T> {
            private final Consumer<T> consumer;

            private OfConsumer(SingleValueCache<T> parent, Consumer<T> consumer) {
                super(parent);

                this.consumer = consumer;
            }

            @Override
            public void acceptNewValue(T value) {
                consumer.accept(value);
            }
        }
    }
}
