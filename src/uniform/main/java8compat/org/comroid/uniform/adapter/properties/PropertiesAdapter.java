package org.comroid.uniform.adapter.properties;

import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

public class PropertiesAdapter extends UniObjectNode.Adapter<Properties> {
    protected PropertiesAdapter(Properties data) {
        super(data);
    }

    @Override
    public Object put(String key, Object value) {
        return getBaseNode().put(key, value);
    }

    @Override
    public @NotNull
    Set<Entry<String, Object>> entrySet() {
        return getBaseNode().entrySet()
                .stream()
                .map(entry -> new AbstractMap.SimpleImmutableEntry<>(String.valueOf(entry.getKey()), entry.getValue()))
                .collect(Collectors.toSet());
    }
}
