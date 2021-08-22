package org.comroid.mutatio.ref;

import org.comroid.mutatio.model.KeyRef;
import org.comroid.mutatio.model.ReferenceOverwriter;
import org.comroid.mutatio.stack.RefStack;
import org.comroid.mutatio.stack.RefStackUtil;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class KeyedReference<K, V> extends Reference<V> implements KeyRef<K, V> {
    //region Stack Accessors
    public static final int KEY_INDEX = 1;
    public static final int VALUE_INDEX = 0;

    @Override
    public final K getKey() {
        return keyStack().get();
    }

    @Override
    public final boolean setKey(K key) {
        return keyStack().set(key);
    }

    @Override
    public final V getValue() throws ClassCastException {
        return valueStack().get();
    }

    @Override
    public final V setValue(V value) {
        RefStack<V> stack = valueStack();
        V prev = stack.get();
        if (!stack.set(value))
            return null;
        return prev;
    }

    public final RefStack<K> keyStack() {
        return this.stack(KEY_INDEX, true);
    }

    public final RefStack<V> valueStack() {
        return this.stack(VALUE_INDEX, true);
    }
    //endregion

    //region Constructors
    public KeyedReference(K key, boolean mutable) {
        this(key, null, mutable);
    }

    public KeyedReference(K key, V value, boolean mutable) {
        super(2);
        setKey(key);
        setValue(value);
        setMutable(mutable);
    }

    public KeyedReference(RefStack<K> keyStack, RefStack<V> valueStack) {
        this(false, keyStack, valueStack);
    }

    public KeyedReference(boolean mutable, RefStack<K> keyStack, RefStack<V> valueStack) {
        super(mutable, keyStack, valueStack);
    }
    //endregion

    //region Static Methods
    private final static KeyedReference<?, ?> EMPTY = createKey(false, null, null);

    public static <K, V> KeyedReference<K, V> emptyKey() {
        //noinspection unchecked
        return (KeyedReference<K, V>) EMPTY;
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

    public static <K, V> KeyedReference<K, V> createKey(boolean mutable, K key, @Nullable V value) {
        if (!mutable && (key == null && value == null) && EMPTY != null)
            return emptyKey();
        return new KeyedReference<>(key, value, mutable);
    }

    public static <K, V> KeyedReference<K, V> conditional(
            BooleanSupplier condition,
            Supplier<K> keySupplier,
            Supplier<V> valueSupplier
    ) {
        return new KeyedReference<>(
                RefStackUtil.$conditional(condition, keySupplier),
                RefStackUtil.$conditional(condition, valueSupplier)
        );
    }
    //endregion

    public final void consume(BiConsumer<? super K, ? super V> consumer) {
        consumer.accept(getKey(), getValue());
    }

    public interface Advancer<IK, IV, OK, OV> extends ReferenceOverwriter<IV, OV, KeyedReference<IK, IV>, KeyedReference<OK, OV>> {
        @Override
        KeyedReference<OK, OV> advance(KeyedReference<IK, IV> reference);
    }
}
