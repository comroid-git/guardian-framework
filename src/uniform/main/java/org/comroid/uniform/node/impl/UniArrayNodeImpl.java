package org.comroid.uniform.node.impl;

import org.comroid.api.HeldType;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Stream;

public final class UniArrayNodeImpl
        extends AbstractUniNode<Integer, KeyedReference<Integer, UniNode>, List<Object>>
        implements UniArrayNode {
    public <BAS, ARR extends BAS> UniArrayNodeImpl(SerializationAdapter<BAS, ?, ARR> seriLib, List<Object> baseNode) {
        super(seriLib, baseNode);
    }

    @Override
    public boolean contains(Object other) {
        return stream().anyMatch(other::equals);
    }

    @Override
    public Stream<UniNode> stream() {
        return streamNodes().map(UniNode.class::cast);
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
                .filter(ref -> ref.testIfPresent(other::equals))
                .findAny()
                .map(Map.Entry::getKey)
                .map(index -> accessors.getReference((int) index, true).unset())
                .orElse(false);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> other) {
        return accessors.stream().allMatch(other::contains);
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
    public UniNode set(int index, UniNode element) {
        KeyedReference<Integer, UniNode> ref = accessors.compute(index, r -> {
            if (r == null)
                //noinspection unchecked
                return (KeyedReference<Integer, UniNode>) generateAccessor(index);
            return r;
        });
        return ref.setValue(element);
    }

    @Override
    public void add(int index, UniNode element) {
        set(index, element);
    }

    @Override
    public @NotNull <T> UniNode put(int index, HeldType<T> type, T value) throws UnsupportedOperationException {
        return accessors.compute(index, ref -> {
            if (value instanceof UniObjectNode || value instanceof UniArrayNode) {
                return KeyedReference.create(index, (UniNode) value);
            } else {
                //noinspection unchecked
                final UniValueNodeImpl valueNode = new UniValueNodeImpl(String.valueOf(index), seriLib, seriLib.createValueAdapter(value));
                if (ref != null) {
                    ref.set(valueNode);
                    return ref;
                }
                return KeyedReference.create(index, valueNode);
            }
        }).getValue();
    }

    @Override
    public UniNode remove(int index) {
        KeyedReference<? super Integer, KeyedReference<Integer, UniNode>> ref
                = accessors.getReference(index, false);
        UniNode node = null;
        if (ref != null) {
            node = ref.into(Rewrapper::get);
            ref.unset();
        }
        return node;
    }

    @Override
    public int indexOf(Object other) {
        for (int i = 0; i < accessors.size(); i++) {
            if (other.equals(accessors.get(i).getValue()))
                return i;
        }
        return -1;
    }

    @Override
    public int lastIndexOf(Object other) {
        for (int i = size() - 1; i >= 0; i--) {
            if (other.equals(accessors.get(i).getValue()))
                return i;
        }
        return -1;
    }

    @NotNull
    @Override
    public ListIterator<UniNode> listIterator() {
        throw new UnsupportedOperationException(); // todo
    }

    @NotNull
    @Override
    public ListIterator<UniNode> listIterator(int index) {
        throw new UnsupportedOperationException(); // todo
    }

    @NotNull
    @Override
    public List<UniNode> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException(); // todo
    }

    @Override
    protected KeyedReference<Integer, ? extends UniNode> generateAccessor(Integer key) {
        return new KeyedReference.Support.Base<Integer, UniNode>(false, key, null) {
            @Override
            public boolean isOutdated() {
                return true;
            }

            @Override
            protected UniNode doGet() {
                final Object value = baseNode.get(key);
                assert getNodeType() == NodeType.ARRAY;
                assert value instanceof List;
                //noinspection unchecked
                return new UniArrayNodeImpl(seriLib, ((List<Object>) value));
            }

            @Override
            protected boolean doSet(UniNode value) {
                switch (value.getNodeType()) {
                    case OBJECT:
                        Map<String, Object> map = new HashMap<>(value.asObjectNode());
                        return baseNode.set(key, map) != value;
                    case ARRAY:
                        ArrayList<UniNode> list = new ArrayList<>(value.asArrayNode());
                        return baseNode.set(key, list) != value;
                }

                return baseNode.set(key, value.asRaw(null)) != value;
            }
        };
    }
}
