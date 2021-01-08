package org.comroid.mutatio.ref;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public interface KeyedReference<K, V> extends Reference<V>, Map.Entry<K, V> {
    @Override
    default V getValue() {
        return get();
    }

    static <K, V> KeyedReference<K, V> create(K key) {
        return create(key, null);
    }

    static <K, V> KeyedReference<K, V> create(K key, @Nullable V initialValue) {
        return create(true, key, initialValue);
    }

    static <K, V> KeyedReference<K, V> create(boolean mutable, K key) {
        return create(mutable, key, null);
    }

    static <K, V> KeyedReference<K, V> create(boolean mutable, K key, @Nullable V initialValue) {
        return new Support.Base<>(mutable, key, initialValue);
    }

    static <K, V> KeyedReference<K, V> conditional(
            BooleanSupplier condition,
            Supplier<K> keySupplier,
            Supplier<V> valueSupplier
    ) {
        return new Support.Conditional<>(condition, keySupplier, valueSupplier);
    }

    @Override
    default V setValue(V value) {
        V rtrn = null;
        if (isUpToDate())
            rtrn = get();
        set(value);
        return rtrn;
    }

    final class Support {
        public static class Base<K, V> extends Reference.Support.Base<V> implements KeyedReference<K, V> {
            private final K key;
            private final Reference<V> valueHolder;

            @Override
            public K getKey() {
                return key;
            }

            @Override
            public V getValue() {
                return get();
            }

            public Base(K key, Reference<V> valueHolder) {
                super(valueHolder, valueHolder.isMutable());

                this.key = key;
                this.valueHolder = valueHolder;
            }

            protected Base(boolean mutable, K key, @Nullable V initialValue) {
                super(mutable);

                this.key = key;
                this.valueHolder = Reference.create(initialValue);
            }

            @Override
            public V setValue(V value) {
                V prev = get();

                return set(value) ? prev : null;
            }

            @Override
            protected V doGet() {
                return valueHolder.get();
            }

            @Override
            protected boolean doSet(V value) {
                return valueHolder.set(value);
            }
        }

        private static final class Conditional<K, V> extends Support.Base<K, V> {
            private final BooleanSupplier condition;
            private final Supplier<K> keySupplier;
            private final Supplier<V> valueSupplier;

            @Override
            public boolean isOutdated() {
                return true;
            }

            public Conditional(
                    BooleanSupplier condition,
                    Supplier<K> keySupplier,
                    Supplier<V> valueSupplier
            ) {
                super(false, null, null);

                this.condition = condition;
                this.keySupplier = keySupplier;
                this.valueSupplier = valueSupplier;
            }


            @Override
            protected V doGet() {
                if (condition.getAsBoolean())
                    return valueSupplier.get();
                return null;
            }

            @Override
            public K getKey() {
                return keySupplier.get();
            }
        }
    }
}
