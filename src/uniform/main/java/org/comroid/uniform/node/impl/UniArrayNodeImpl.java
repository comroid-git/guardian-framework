package org.comroid.uniform.node.impl;

import org.comroid.api.HeldType;
import org.comroid.mutatio.ref.Processor;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UniArrayNodeImpl extends UniNodeBase implements UniArrayNode {
    private final Map<Integer, UniValueNode<String>> valueAdapters = new ConcurrentHashMap<>();
    private final Adapter adapter;

    @Override
    public final Object getBaseNode() {
        return adapter.getBaseNode();
    }

    public UniArrayNodeImpl(SerializationAdapter<?, ?, ?> serializationAdapter, Adapter adapter) {
        super(serializationAdapter, Type.ARRAY);

        this.adapter = adapter;
    }

    @Override
    public @NotNull UniNode get(int index) {
        return makeValueNode(index).orElseGet(UniValueNode::nullNode);
    }

    private Processor<UniNode> makeValueNode(int index) {
        class Accessor extends Reference.Support.Base<String> {
            private final int index;

            private Accessor(int index) {
                super(true);

                this.index = index;
            }

            @Nullable
            @Override
            public String doGet() {
                return unwrapDST(adapter.get(index));
            }

            @Override
            protected boolean doSet(String newValue) {
                return adapter.set(index, newValue) != newValue;
            }
        }

        String key = String.valueOf(index);
        return computeNode(key, () -> new Accessor(index));
    }

    @Override
    public UniArrayNode copyFrom(@NotNull UniNode it) {
        if (it instanceof UniArrayNode) {
            //noinspection unchecked
            adapter.addAll(((UniArrayNode) it).adapter);
            return this;
        }
        return unsupported(this, "COPY_FROM", Type.ARRAY);
    }

    @Override
    public int size() {
        return adapter.size();
    }

    @Override
    public boolean has(String fieldName) {
        return unsupported(this, "HAS_FIELD", Type.OBJECT);
    }

    @Override
    public @NotNull UniNode get(String fieldName) {
        return unsupported(this, "GET_FIELD", Type.OBJECT);
    }

    @Override
    public @NotNull <T> UniNode put(int index, HeldType<T> type, T value) {
        UniNode node = unwrapNode(String.valueOf(index), type, value);
        if (node != null)
            return node;

        if (type == ValueTypeBase.VOID) {
            adapter.set(index, value);
            return get(index);
        } else {
            final String put = type.convert(value, ValueTypeBase.STRING);

            final UniNodeBase vn = makeValueNode(index).requireNonNull("Missing Node");
            vn.set(put);
            return vn;
        }
    }

    @Override
    public @NotNull UniObjectNode putObject(int index) {
        final UniObjectNode objectNode = serializationAdapter.createUniObjectNode();
        adapter.add(index, objectNode.getBaseNode());
        return objectNode;
    }

    @Override
    public @NotNull UniArrayNode putArray(int index) {
        final UniArrayNode arrayNode = serializationAdapter.createUniArrayNode();
        adapter.add(index, arrayNode.getBaseNode());
        return arrayNode;
    }

    @Override
    public synchronized List<Object> asList() {
        return adapter;
    }

    @Override
    public synchronized List<? extends UniNode> asNodeList() {
        final List<UniNode> yields = new ArrayList<>();

        for (int i = 0; i < size(); i++) {
            yields.add(get(i));
        }

        return yields;
    }

    public static abstract class Adapter<B> extends AbstractList<Object> implements UniNodeBase.Adapter<B> {
        protected final B baseNode;

        @Override
        public B getBaseNode() {
            return baseNode;
        }

        protected Adapter(B baseNode) {
            this.baseNode = baseNode;
        }

        @Override
        public abstract Object get(int index);

        @Override
        public abstract Object set(int index, Object element);

        @Override
        public abstract void add(int index, Object element);

        @Override
        public abstract Object remove(int index);
    }
}
