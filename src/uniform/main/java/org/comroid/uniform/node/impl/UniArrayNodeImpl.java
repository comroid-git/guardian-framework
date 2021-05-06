package org.comroid.uniform.node.impl;

import org.comroid.api.Polyfill;
import org.comroid.api.ValueType;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class UniArrayNodeImpl
        extends AbstractUniNode<Integer, KeyedReference<Integer, UniNode>, List<Object>>
        implements UniArrayNode {
    public <BAS, ARR extends BAS> UniArrayNodeImpl(SerializationAdapter<BAS, ?, ARR> seriLib, @Nullable UniNode parent, List<Object> baseNode) {
        super(seriLib, parent, baseNode);
    }

    @Override
    public boolean contains(Object value) {
        return streamNodes().anyMatch(node -> node.asRaw().equals(value));
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return toArray(new Object[0]);
    }

    @NotNull
    @Override
    public <T> T[] toArray(final @NotNull T[] a) {
        return stream().map(Polyfill::uncheckedCast).toArray(x -> a);
    }

    @Override
    public boolean add(UniNode uniNode) {
        return set(size(), uniNode) != uniNode;
    }

    @Override
    public boolean remove(Object other) {
        return accessors.streamRefs()
                .filter(ref -> ref.contentEquals(other))
                .findAny()
                .map(Map.Entry::getKey)
                .map(index -> accessors.getReference(index, true).unset())
                .orElse(false);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> other) {
        return streamRefs().allMatch(other::contains);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends UniNode> other) {
        //noinspection ReplaceInefficientStreamCount
        return other.stream()
                .filter(this::add)
                .count() != 0;
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends UniNode> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return removeIf(c::contains);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return removeIf(it -> !c.contains(it));
    }

    @Override
    public UniNode set(int index, UniNode value) {
        ValueType<UniNode> nodetype = Polyfill.uncheckedCast(
                value.getNodeType() == NodeType.OBJECT
                        ? seriLib.getObjectType()
                        : seriLib.getArrayType());
        return put(index, nodetype, value);
    }

    @Override
    public void add(int index, UniNode element) {
        throw new UnsupportedOperationException("Please use method #set()"); // todo
    }

    @Override
    public @NotNull <T> UniNode put(final int index, ValueType<T> type, T value) throws UnsupportedOperationException {
        return Objects.requireNonNull(accessors.compute(index, (k, v) -> {
            if (value == null)
                return null;
            else if (value instanceof UniObjectNode || value instanceof UniArrayNode)
                return (UniNode) value;
            else {
                UniValueNodeImpl valueNode = new UniValueNodeImpl(String.valueOf(index), seriLib, this, seriLib
                        .createValueAdapter(value, nv -> baseNode.set(index, nv) != nv));
                return valueNode;
            }
        }));
    }

    @Override
    public UniNode remove(int index) {
        KeyedReference<Integer, UniNode> ref
                = accessors.getReference(index, false);
        UniNode node = null;
        if (ref != null) {
            node = ref.get();
            ref.unset();
        }
        return node;
    }

    @Override
    public int indexOf(Object other) {
        for (int i = 0; i < accessors.size(); i++) {
            if (other.equals(accessors.get(i)))
                return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object other) {
        for (int i = size() - 1; i >= 0; i--) {
            if (other.equals(accessors.get(i)))
                return i;
        }
        return -1;
    }

    @NotNull
    @Override
    public ListIterator listIterator() {
        return listIterator(0);
    }

    @NotNull
    @Override
    public ListIterator listIterator(int index) {
        return new ListIterator(index);
    }

    @NotNull
    @Override
    public List<UniNode> subList(int fromIndex, int toIndex) {
        return Collections.unmodifiableList(IntStream
                .range(fromIndex, toIndex)
                .mapToObj(this::get)
                .collect(Collectors.toList()));
    }

    @Override
    protected KeyedReference<Integer, UniNode> generateAccessor(Integer key) {
        return new KeyedReference.Support.Base<Integer, UniNode>(key, null, true) {
            /*
                        @Override
                        public boolean isOutdated() {
                            return true;
                        }
            */
            @Override
            protected UniNode doGet() {
                if (key < 0 || key >= baseNode.size())
                    return UniValueNode.NULL;
                final Object value = baseNode.get(key);

                assert getNodeType() == NodeType.ARRAY;

                if (value == null)
                    return UniValueNode.NULL;
                if (seriLib.getObjectType().test(value)) {
                    // value is object
                    return seriLib.createObjectNode(value);
                } else if (seriLib.getArrayType().test(value)) {
                    // value is array
                    return seriLib.createArrayNode(value);
                } else return new UniValueNodeImpl(key.toString(), seriLib, UniArrayNodeImpl.this, seriLib
                        .createValueAdapter(value, nv -> baseNode.set(key, nv) != nv));
            }

            @Override
            protected boolean doSet(UniNode value) {
                if (value instanceof UniValueNode)
                    return baseNode.set(key, value.asRaw()) != value;
                return baseNode.set(key, value.getBaseNode()) != value;
            }
        };
    }

    @Override
    public int size() {
        if (baseNode == null)
            return 0;
        return baseNode.size();
    }

    @Override
    protected Stream<Integer> streamKeys() {
        return IntStream.range(0, baseNode.size()).boxed();
    }

    private final class ListIterator implements java.util.ListIterator<UniNode> {
        private final Reference<Integer> index;

        private ListIterator(int initialIndex) {
            this.index = Reference.create(initialIndex);
        }

        @Override
        public boolean hasNext() {
            return index.test(x -> size() < x);
        }

        @Override
        public UniNode next() {
            index.compute(x -> x + 1);
            return index.into(UniArrayNodeImpl.this::get);
        }

        @Override
        public boolean hasPrevious() {
            return index.test(x -> -1 > x);
        }

        @Override
        public UniNode previous() {
            index.compute(x -> x - 1);
            return index.into(UniArrayNodeImpl.this::get);
        }

        @Override
        public int nextIndex() {
            return index.into(x -> x + 1);
        }

        @Override
        public int previousIndex() {
            return index.into(x -> x + 1);
        }

        @Override
        public void remove() {
            index.into(UniArrayNodeImpl.this::remove);
        }

        @Override
        public void set(UniNode uniNode) {
            index.consume(index -> UniArrayNodeImpl.this.set(index, uniNode));
        }

        @Override
        public void add(UniNode uniNode) {
            index.consume(index -> UniArrayNodeImpl.this.add(index, uniNode));
        }
    }
}
