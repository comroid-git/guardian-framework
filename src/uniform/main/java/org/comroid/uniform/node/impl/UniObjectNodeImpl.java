package org.comroid.uniform.node.impl;

import org.comroid.api.HeldType;
import org.comroid.mutatio.ref.Processor;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class UniObjectNodeImpl extends UniNodeBase implements UniObjectNode {
    private final Adapter adapter;

    @Override
    public final Object getBaseNode() {
        return adapter.getBaseNode();
    }

    public UniObjectNodeImpl(SerializationAdapter<?, ?, ?> serializationAdapter, Adapter adapter) {
        super(serializationAdapter, Type.OBJECT);

        this.adapter = adapter;
    }

    public static UniObjectNode ofMap(SerializationAdapter<?, ?, ?> adapter, Map<String, Object> map) {
        class MergedAdapter extends UniObjectNode.Adapter<Map<String, Object>> {
            protected MergedAdapter(Map<String, Object> underlying) {
                super(underlying);
            }

            @Override
            public Object put(String key, Object value) {
                return getBaseNode().put(key, value);
            }

            @Override
            public @NotNull Set<Entry<String, Object>> entrySet() {
                return getBaseNode().entrySet();
            }
        }

        return new UniObjectNode(adapter, new MergedAdapter(map));
    }

    public static UniObjectNode dummy() {
        return ofMap(null, new HashMap<>());
    }

    @Override
    public @NotNull UniNode get(int index) {
        return unsupported("GET_INDEX", Type.ARRAY);
    }

    @Override
    public int size() {
        return unsupported("SIZE", Type.ARRAY);
    }

    @Override
    public boolean has(String fieldName) {
        if (fieldName.isEmpty())
            return true;
        return adapter.containsKey(fieldName);
    }

    @Override
    public @NotNull UniNode get(final String fieldName) {
        if (fieldName.isEmpty())
            return this;
        return makeValueNode(fieldName).orElseGet(UniValueNode::empty);
    }

    private Processor<UniNode> makeValueNode(String fieldName) {
        return computeNode(fieldName, () -> new KeyAccessor(fieldName));
    }

    @Override
    public @NotNull <T> UniNode put(String key, HeldType<T> type, T value) {
        UniNode node = unwrapNode(key, type, value);
        if (node != null)
            return node;

        if (type == ValueType.VOID) {
            adapter.put(key, value);
            return get(key);
        } else {
            final String put = type.convert(value, ValueType.STRING);

            final UniNodeBase vn = makeValueNode(key).requireNonNull("Missing Node");
            vn.set(put);
            return vn;
        }
    }

    @Override
    public UniNode putNull(String key) throws UnsupportedOperationException {
        final UniNode nullNode = serializationAdapter.createUniNode(null);

        adapter.put(key, nullNode);
        return nullNode;
    }

    @Override
    public @NotNull UniObjectNode putObject(String key) {
        final UniObjectNode objectNode = serializationAdapter.createUniObjectNode();

        adapter.put(key, objectNode.getBaseNode());
        return objectNode;
    }

    @Override
    public @NotNull UniArrayNode putArray(String key) {
        final UniArrayNode arrayNode = serializationAdapter.createUniArrayNode();
        adapter.put(key, arrayNode.getBaseNode());
        return arrayNode;
    }

    @Override
    public UniObjectNode copyFrom(@NotNull UniNode it) {
        if (it instanceof UniObjectNode) {
            //noinspection unchecked
            adapter.putAll(((UniObjectNode) it).adapter);
            return this;
        }
        return unsupported("COPY_FROM", Type.OBJECT);
    }

    public Map<String,Object> asMap() {
        return adapter;
    }

    public static abstract class Adapter<B> extends AbstractMap<String, Object> implements UniNodeBase.Adapter<B> {
        protected final B baseNode;

        @Override
        public B getBaseNode() {
            return baseNode;
        }

        protected Adapter(B baseNode) {
            this.baseNode = baseNode;
        }

        @Override
        public abstract Object put(String key, Object value);

        @NotNull
        @Override
        public abstract Set<Entry<String, Object>> entrySet();
    }

    private final class KeyAccessor extends Reference.Support.Base<String> {
        private final String fieldName;

        @Override
        public boolean isOutdated() {
            final String newer = doGet();
            return atom.compareAndSet(newer, newer);
        }

        protected KeyAccessor(String fieldName) {
            super(true);

            this.fieldName = fieldName;
        }

        @Override
        protected String doGet() {
            return String.valueOf(adapter.getOrDefault(fieldName, null));
        }

        @Override
        protected boolean doSet(String value) {
            return adapter.put(fieldName, value) != value;
        }
    }
}
