package org.comroid.mutatio.cache;

import org.comroid.api.UncheckedCloseable;

import java.util.function.Consumer;

public interface ValueUpdateListener<T> extends UncheckedCloseable {
    static <T> ValueUpdateListener<T> ofConsumer(ValueCache<T> parent, Consumer<T> consumer) {
        return new Support.OfConsumer<>(parent, consumer);
    }

    void acceptNewValue(T value);

    final class Support {
        public static abstract class Base<T> implements ValueUpdateListener<T> {
            private final ValueCache<T> parent;

            public Base(ValueCache<T> parent) {
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

            private OfConsumer(ValueCache<T> parent, Consumer<T> consumer) {
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
