package org.comroid.util;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public final class MapUtil {
    public static <K, V> Map<K, V> hashtable(Hashtable<K, V> hashtable) {
        return new AbstractMap<K, V>() {
            private final Hashtable<K, V> table = hashtable;

            @NotNull
            @Override
            public Set<Entry<K, V>> entrySet() {
                return table.entrySet();
            }

            @Override
            public V put(K key, V value) {
                return table.put(key, value);
            }
        };
    }
}
