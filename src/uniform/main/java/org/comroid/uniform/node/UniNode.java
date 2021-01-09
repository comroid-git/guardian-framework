package org.comroid.uniform.node;

import org.comroid.api.*;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.ref.Processor;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.model.SerializationAdapterHolder;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface UniNode extends Specifiable<UniNode>, SerializationAdapterHolder {
    @Internal
    static <T> T unsupported(UniNode it, String actionName, Type expected) throws UnsupportedOperationException {
        throw new UnsupportedOperationException(String.format("Cannot invoke %s on node type %s; " + "%s expected",
                actionName,
                it.getType(),
                expected
        ));
    }

    default String getSerializedString() {
        return toString();
    }

    Type getType();

    default boolean isObjectNode() {
        return getType() == Type.OBJECT;
    }

    default boolean isArrayNode() {
        return getType() == Type.ARRAY;
    }

    default boolean isValueNode() {
        return getType() == Type.VALUE;
    }

    default boolean isNull() {
        return unsupported(this, "IS_NULL", Type.VALUE);
    }

    String getMimeType();

    default boolean isNotNull() {
        return !isNull();
    }

    int size();

    default boolean isNull(String fieldName) {
        return wrap(fieldName).map(UniNode::isNull)
                .orElse(true);
    }

    @NotNull UniNode get(int index);

    @NotNull UniNode get(String fieldName);

    @NotNull
    default Optional<UniNode> wrap(int index) {
        return has(index) ? Optional.of(get(index)) : Optional.empty();
    }

    @NotNull
    default Optional<UniNode> wrap(String fieldName) {
        return has(fieldName) ? Optional.of(get(fieldName)) : Optional.empty();
    }

    @NotNull
    default Processor<UniNode> process(int index) {
        return Processor.ofConstant(get(index));
    }

    @NotNull
    default Processor<UniNode> process(String fieldName) {
        return Processor.ofConstant(get(fieldName));
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
    default <T> UniNode add(HeldType<T> type, T value) throws UnsupportedOperationException {
        return put(size(), type, value);
    }

    @NotNull
    default <T> UniNode put(int index, HeldType<T> type, T value) throws UnsupportedOperationException {
        return unsupported(this, "PUT_INDEX", Type.ARRAY);
    }

    @NotNull
    default <B extends Named & ValuePointer<T>, T> UniNode put(B idBox, T value) {
        return put(idBox.getName(), idBox.getHeldType(), value);
    }

    @NotNull
    default <T> UniNode put(String key, HeldType<T> type, T value) throws UnsupportedOperationException {
        return unsupported(this, "PUT_KEY", Type.OBJECT);
    }

    @NotNull
    default UniNode addNull() throws UnsupportedOperationException {
        return putNull(size());
    }

    @NotNull
    default UniNode putNull(int index) throws UnsupportedOperationException {
        return unsupported(this, "PUT_NULL_INDEX", Type.ARRAY);
    }

    @NotNull
    default UniNode putNull(String key) throws UnsupportedOperationException {
        return unsupported(this, "PUT_NULL_KEY", Type.OBJECT);
    }

    @NotNull
    default UniObjectNode addObject() throws UnsupportedOperationException {
        return putObject(size());
    }

    @NotNull
    default UniObjectNode putObject(int index) throws UnsupportedOperationException {
        return unsupported(this, "PUT_OBJECT_INDEX", Type.ARRAY);
    }

    @NotNull
    default UniObjectNode putObject(String key) throws UnsupportedOperationException {
        return unsupported(this, "PUT_OBJECT_KEY", Type.OBJECT);
    }

    @NotNull
    default UniArrayNode addArray() throws UnsupportedOperationException {
        return putArray(size());
    }

    @NotNull
    default UniArrayNode putArray(int index) throws UnsupportedOperationException {
        return unsupported(this, "PUT_ARRAY_INDEX", Type.ARRAY);
    }

    @NotNull
    default UniArrayNode putArray(String key) throws UnsupportedOperationException {
        return unsupported(this, "PUT_ARRAY_KEY", Type.ARRAY);
    }

    @Contract(value = "_ -> this", mutates = "this")
    UniNode copyFrom(@NotNull UniNode it);

    default Object asRaw(@Nullable Object fallback) {
        if (isNull() && fallback != null) {
            return fallback;
        }

        return unsupported(this, "GET_RAW", Type.VALUE);
    }

    default <R> R as(ValueType<R> type) {
        return unsupported(this, "GET_AS", Type.VALUE);
    }

    default String asString() {
        return asString(null);
    }

    default String asString(@Nullable String fallback) {
        if (isNull() && fallback != null) {
            return fallback;
        }

        return unsupported(this, "GET_AS_STRING", Type.VALUE);
    }

    default boolean asBoolean() {
        return asBoolean(false);
    }

    default boolean asBoolean(boolean fallback) {
        if (isNull()) {
            return fallback;
        }

        return unsupported(this, "GET_AS_BOOLEAN", Type.VALUE);
    }

    default int asInt() {
        return asInt(0);
    }

    default int asInt(int fallback) {
        if (isNull()) {
            return fallback;
        }

        return unsupported(this, "GET_AS_INT", Type.VALUE);
    }

    default long asLong() {
        return asLong(0);
    }

    default long asLong(long fallback) {
        if (isNull()) {
            return fallback;
        }

        return unsupported(this, "GET_AS_LONG", Type.VALUE);
    }

    default double asDouble() {
        return asDouble(0);
    }

    default double asDouble(double fallback) {
        if (isNull()) {
            return fallback;
        }

        return unsupported(this, "GET_AS_DOUBLE", Type.VALUE);
    }

    default float asFloat() {
        return asFloat(0);
    }

    default float asFloat(float fallback) {
        if (isNull()) {
            return fallback;
        }

        return unsupported(this, "GET_AS_FLOAT", Type.VALUE);
    }

    default short asShort() {
        return asShort((short) 0);
    }

    default short asShort(short fallback) {
        if (isNull()) {
            return fallback;
        }

        return unsupported(this, "GET_AS_SHORT", Type.VALUE);
    }

    default char asChar() {
        return asChar((char) 0);
    }

    default char asChar(char fallback) {
        if (isNull()) {
            return fallback;
        }

        return unsupported(this, "GET_AS_CHAR", Type.VALUE);
    }

    default List<Object> asList() {
        return unsupported(this, "GET_AS_LIST", Type.ARRAY);
    }

    default List<? extends UniNode> asNodeList() {
        return unsupported(this, "GET_AS_NODELIST", Type.ARRAY);
    }

    default UniObjectNode asObjectNode() {
        return as(UniObjectNode.class, MessageSupplier.format("Node is of %s type; expected %s", getType(), Type.OBJECT));
    }

    default UniArrayNode asArrayNode() {
        return as(UniArrayNode.class, MessageSupplier.format("Node is of %s type; expected %s", getType(), Type.ARRAY));
    }

    default <T> UniValueNode<T> asValueNode() {
        return as(UniValueNode.class, MessageSupplier.format("Node is of %s type; expected %s", getType(), Type.VALUE));
    }

    enum Type {
        OBJECT(DataStructureType.Primitive.OBJECT),
        ARRAY(DataStructureType.Primitive.ARRAY),
        VALUE(null);

        public final @Nullable DataStructureType.Primitive dst;

        Type(@Nullable DataStructureType.Primitive primitive) {
            this.dst = primitive;
        }
    }
}
