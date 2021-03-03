package org.comroid.mutatio.ref;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.*;

public abstract class KeyedReference<K, V> extends Reference<V> implements Map.Entry<K, V> {
    private final K key;
    private final Reference<V> valueHolder;

    @Override
    public V getValue() {
        return get();
    }

    @Override
    public K getKey() {
        return key;
    }

    public KeyedReference(K key, Reference<V> valueHolder) {
        super(valueHolder, valueHolder.isMutable(), valueHolder.getAutocomputor());

        this.key = key;
        this.valueHolder = valueHolder;
    }

    protected KeyedReference(K key, boolean mutable) {
        this(key, null, mutable);
    }

    protected KeyedReference(K key, boolean mutable, Executor autoComputor) {
        this(key, null, mutable, autoComputor);
    }

    protected KeyedReference(K key, @Nullable V initialValue, boolean mutable) {
        this(key, initialValue, mutable, null);
    }

    protected KeyedReference(K key, @Nullable V initialValue, boolean mutable, Executor autoComputor) {
        super(null, mutable, autoComputor);

        this.key = key;
        this.valueHolder = Reference.create(initialValue);
    }

    public static <K, V> KeyedReference<K, V> emptyKey() {
        //noinspection unchecked
        return (KeyedReference<K, V>) Support.EMPTY;
    }

    public static <K, V> KeyedReference<K, V> emptyValue(K key) {
        return createKey(false, key);
    }

    public static <K, V> KeyedReference<K, V> createKey(K key) {
        return createKey(key, null);
    }

    public static <K, V> KeyedReference<K, V> createKey(K key, @Nullable V initialValue) {
        return createKey(true, key, initialValue);
    }

    public static <K, V> KeyedReference<K, V> createKey(boolean mutable, K key) {
        return createKey(mutable, key, null);
    }

    public static <K, V> KeyedReference<K, V> createKey(boolean mutable, K key, @Nullable V initialValue) {
        return new Support.Base<>(key, initialValue, mutable);
    }

    public static <K, V> KeyedReference<K, V> conditional(
            BooleanSupplier condition,
            Supplier<K> keySupplier,
            Supplier<V> valueSupplier
    ) {
        return new Support.Conditional<>(condition, keySupplier, valueSupplier);
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

    public interface Advancer<IK, IV, OK, OV> extends ReferenceOverwriter<IV, OV, KeyedReference<IK, IV>, KeyedReference<OK, OV>> {
        @Override
        KeyedReference<OK, OV> advance(KeyedReference<IK, IV> reference);
    }

    public static final class Support {
        public static final KeyedReference<?, ?> EMPTY = createKey(false, null);

        public static class Base<K, V> extends KeyedReference<K, V> {
            public Base(K key, Reference<V> valueHolder) {
                super(key, valueHolder);
            }

            protected Base(K key, @Nullable V initialValue, boolean mutable) {
                this(key, initialValue, mutable, null);
            }

            protected Base(K key, @Nullable V initialValue, boolean mutable, Executor autoComputor) {
                super(key, initialValue, mutable, autoComputor);
            }
        }

        public static final class Filtered<K, V> extends Support.Base<K, V> {
            private final KeyedReference<K, V> parent;
            private final BiPredicate<? super K, ? super V> filter;

            @Deprecated
            public Filtered(
                    KeyedReference<K, V> parent,
                    final Predicate<? super K> keyFilter,
                    final Predicate<? super V> valueFilter
            ) {
                this(parent, (k, v) -> keyFilter.test(k) && valueFilter.test(v));
            }

            public Filtered(
                    KeyedReference<K, V> parent,
                    BiPredicate<? super K, ? super V> filter
            ) {
                super(parent.getKey(), parent);
                this.parent = parent;
                this.filter = filter;
            }

            @Override
            protected V doGet() {
                if (filter.test(getKey(), getFromParent()))
                    return parent.getValue();
                return null;
            }
        }

        public static final class Mapped<InK, InV, K, V> extends Support.Base<K, V> {
            private final KeyedReference<InK, InV> parent;
            private final Function<? super InK, ? extends K> keyMapper;

            @Override
            public K getKey() {
                return keyMapper.apply(parent.getKey());
            }

            public Mapped(
                    KeyedReference<InK, InV> parent,
                    Function<? super InK, ? extends K> keyMapper,
                    BiFunction<? super InK, ? super InV, ? extends V> valueMapper
            ) {
                super(null, parent.map(in -> valueMapper.apply(parent.getKey(), in)));

                this.parent = parent;
                this.keyMapper = keyMapper;
            }
        }

        private static final class Conditional<K, V> extends Support.Base<K, V> {
            private final BooleanSupplier condition;
            private final Supplier<K> keySupplier;
            private final Supplier<V> valueSupplier;

            @Override
            public K getKey() {
                return keySupplier.get();
            }


            /*
                        @Override
                        public boolean isOutdated() {
                            return true;
                        }
            */
            public Conditional(
                    BooleanSupplier condition,
                    Supplier<K> keySupplier,
                    Supplier<V> valueSupplier
            ) {
                super(null, null, false);

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
        }
    }
}
