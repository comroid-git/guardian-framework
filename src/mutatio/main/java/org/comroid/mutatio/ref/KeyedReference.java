package org.comroid.mutatio.ref;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

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
        return new Basic<>(mutable, key, initialValue);
    }

    @Override
    default V setValue(V value) {
        V rtrn = null;
        if (isUpToDate())
            rtrn = get();
        set(value);
        return rtrn;
    }

    class Basic<K, V> extends Reference.Support.Base<V> implements KeyedReference<K, V> {
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

        public Basic(K key, Reference<V> valueHolder) {
            super(valueHolder.isMutable());

            this.key = key;
            this.valueHolder = valueHolder;
        }

        public Basic(boolean mutable, K key, @Nullable V initialValue) {
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
}
