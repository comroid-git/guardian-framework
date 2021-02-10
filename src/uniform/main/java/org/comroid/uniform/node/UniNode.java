package org.comroid.uniform.node;

import org.comroid.api.*;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.ref.Reference;
import org.comroid.uniform.model.NodeType;
import org.comroid.uniform.model.Serializable;
import org.comroid.uniform.model.SerializationAdapterHolder;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.stream.Stream;

public interface UniNode extends SerializationAdapterHolder, Iterable<UniNode>, Named, Serializable {
    @Override
    default UniNode toUniNode() {
        return this;
    }

    Rewrapper<? extends UniNode> getParentNode();

    @Internal
    static <T> T unsupported(UniNode it, String actionName, NodeType expected) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(String.format("Cannot invoke %s on node type %s; " + "%s expected",
                actionName,
                it.getNodeType(),
                expected
        ));
    }

    default String getSerializedString() {
        return toString();
    }

    NodeType getNodeType();

    default boolean isObjectNode() {
        return getNodeType() == NodeType.OBJECT;
    }

    default boolean isArrayNode() {
        return getNodeType() == NodeType.ARRAY;
    }

    default boolean isValueNode() {
        return getNodeType() == NodeType.VALUE;
    }

    default boolean isNull() {
        return this == UniValueNode.NULL || size() == 0;
    }

    default String getMimeType() {
        return getSerializationAdapter().getMimeType();
    }

    default boolean isNonNull() {
        return !isNull();
    }

    boolean isEmpty();

    int size();

    void clear();

    default boolean isNull(String fieldName) {
        return wrap(fieldName).map(UniNode::isNull).orElse(true);
    }

    @NotNull UniNode get(int index);

    @NotNull UniNode get(String fieldName);

    default @NotNull UniNode get(Named key) {
        return get(key.getName());
    }

    @NotNull
    default Optional<UniNode> wrap(int index) {
        return has(index) ? Optional.of(get(index)) : Optional.empty();
    }

    @NotNull
    default Optional<UniNode> wrap(String fieldName) {
        return has(fieldName) ? Optional.of(get(fieldName)) : Optional.empty();
    }

    @NotNull
    @Deprecated
    default Reference<UniNode> process(int index) {
        return Reference.constant(get(index));
    }

    @NotNull
    @Deprecated
    default Reference<UniNode> process(Named key) {
        return process(key.getName());
    }

    @NotNull
    @Deprecated
    default Reference<UniNode> process(String fieldName) {
        return Reference.constant(get(fieldName));
    }

    default boolean has(int index) {
        return size() < index;
    }

    default boolean has(Named idBox) {
        return has(idBox.getName());
    }

    boolean has(String fieldName);

    // todo: add helper methods
    @NotNull
    default <T> UniNode add(ValueType<T> type, T value) throws UnsupportedOperationException {
        return put(size(), type, value);
    }

    @NotNull
    default <T> UniNode put(int index, ValueType<T> type, T value) throws UnsupportedOperationException {
        return unsupported(this, "PUT_INDEX", NodeType.ARRAY);
    }

    @NotNull
    default <Key extends Named & ValuePointer<T>, Value extends ValueBox<T>, T> UniNode put(Key idBox, Value value) {
        if (!idBox.getHeldType().equals(value.getHeldType()))
            throw new IllegalArgumentException("Unmatching ValueTypes");
        return put(idBox, value.getValue());
    }

    @NotNull
    default <B extends Named & ValuePointer<T>, T> UniNode put(B idBox, T value) {
        return put(idBox.getName(), idBox.getHeldType(), value);
    }

    @NotNull
    default <T> UniNode put(String key, ValueType<? extends T> type, T value) throws UnsupportedOperationException {
        return unsupported(this, "PUT_KEY", NodeType.OBJECT);
    }

    @NotNull
    default UniNode addNull() throws UnsupportedOperationException {
        return putNull(size());
    }

    @NotNull
    default UniNode putNull(int index) throws UnsupportedOperationException {
        return put(index, getFromContext().getValueType(), UniValueNode.NULL);
    }

    @NotNull
    default UniNode putNull(String key) throws UnsupportedOperationException {
        return put(key, getFromContext().getValueType(), UniValueNode.NULL);
    }

    @NotNull
    default UniObjectNode addObject() throws UnsupportedOperationException {
        return putObject(size());
    }

    @NotNull
    default UniObjectNode putObject(int index) throws UnsupportedOperationException {
        final UniObjectNode node = getSerializationAdapter().createObjectNode();
        put(index, getFromContext().getObjectType(), node);
        return node;
    }

    @NotNull
    default UniObjectNode putObject(Named key) throws UnsupportedOperationException {
        return putObject(key.getName());
    }

    @NotNull
    default UniObjectNode putObject(String key) throws UnsupportedOperationException {
        final UniObjectNode node = getSerializationAdapter().createObjectNode();
        put(key, getFromContext().getObjectType(), node);
        return node;
    }

    @NotNull
    default UniArrayNode addArray() throws UnsupportedOperationException {
        return putArray(size());
    }

    @NotNull
    default UniArrayNode putArray(int index) throws UnsupportedOperationException {
        final UniArrayNode node = getSerializationAdapter().createArrayNode();
        put(index, getFromContext().getArrayType(), node);
        return node;
    }

    @NotNull
    default UniArrayNode putArray(Named key) throws UnsupportedOperationException {
        return putArray(key.getName());
    }

    @NotNull
    default UniArrayNode putArray(String key) throws UnsupportedOperationException {
        final UniArrayNode node = getSerializationAdapter().createArrayNode();
        put(key, getFromContext().getArrayType(), node);
        return node;
    }

    @Contract(value = "_ -> this", mutates = "this")
    UniNode copyFrom(@NotNull UniNode it);

    default Object asRaw() {
        return unsupported(this, "GET_RAW", NodeType.VALUE);
    }

    default Object asRaw(@Nullable Object fallback) {
        return fallback;
    }

    default <R> R as(ValueType<R> type) {
        return unsupported(this, "GET_AS", NodeType.VALUE);
    }

    default String asString() {
        return unsupported(this, "GET_AS_STRING", NodeType.VALUE);
    }

    default String asString(@Nullable String fallback) {
        return fallback;
    }

    default boolean asBoolean() {
        return unsupported(this, "GET_AS_BOOLEAN", NodeType.VALUE);
    }

    default boolean asBoolean(boolean fallback) {
        return fallback;
    }

    default int asInt() {
        return unsupported(this, "GET_AS_INT", NodeType.VALUE);
    }

    default int asInt(int fallback) {
        return fallback;
    }

    default long asLong() {
        return unsupported(this, "GET_AS_LONG", NodeType.VALUE);
    }

    default long asLong(long fallback) {
        return fallback;
    }

    default double asDouble() {
        return unsupported(this, "GET_AS_DOUBLE", NodeType.VALUE);
    }

    default double asDouble(double fallback) {
        return fallback;
    }

    default float asFloat() {
        return unsupported(this, "GET_AS_FLOAT", NodeType.VALUE);
    }

    default float asFloat(float fallback) {
        return fallback;
    }

    default short asShort() {
        return unsupported(this, "GET_AS_SHORT", NodeType.VALUE);
    }

    default short asShort(short fallback) {
        return fallback;
    }

    default char asChar() {
        return unsupported(this, "GET_AS_CHAR", NodeType.VALUE);
    }

    default char asChar(char fallback) {
        return fallback;
    }

    default Stream<? extends UniNode> stream() {
        return streamNodes();
    }

    default Stream<? extends UniNode> streamNodes() {
        return streamRefs().flatMap(Reference::stream);
    }

    Stream<? extends Reference<? extends UniNode>> streamRefs();

    default UniObjectNode asObjectNode() {
        return as(UniObjectNode.class, MessageSupplier.format("Node is of %s type; expected %s", getNodeType(), NodeType.OBJECT));
    }

    default UniArrayNode asArrayNode() {
        return as(UniArrayNode.class, MessageSupplier.format("Node is of %s type; expected %s", getNodeType(), NodeType.ARRAY));
    }

    default UniValueNode asValueNode() {
        return as(UniValueNode.class, MessageSupplier.format("Node is of %s type; expected %s", getNodeType(), NodeType.VALUE));
    }

    Object getBaseNode();

    default UniObjectNode computeObject(String key) {
        if (has(key))
            return get(key).asObjectNode();
        return putObject(key);
    }

    default UniObjectNode computeObject(int index) {
        if (has(index))
            return get(index).asObjectNode();
        return putObject(index);
    }

    default UniArrayNode computeArray(String key) {
        if (has(key))
            return get(key).asArrayNode();
        return putArray(key);
    }

    default UniArrayNode computeArray(int index) {
        if (has(index))
            return get(index).asArrayNode();
        return putArray(index);
    }
}
