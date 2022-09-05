package org.comroid.varbind.bind;

import org.comroid.api.Named;
import org.comroid.api.Polyfill;
import org.comroid.api.ValuePointer;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import static org.jetbrains.annotations.ApiStatus.Experimental;

public interface VarBind<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        extends Named, ValuePointer<EXTR> {
    String getFieldName();

    @Override
    default String getName() {
        return getFieldName();
    }

    @Override
    default String getAlternateName() {
        return String.format("VarBind<%s.%s>", getGroup().getName(), getFieldName());
    }

    boolean isRequired();

    Set<VarBind<? extends SELF, ?, ?, ?>> getDependencies();

    GroupBind<SELF> getGroup();

    boolean isListing();

    boolean ignoreInDB();

    boolean identifier();

    @Experimental
    @SuppressWarnings("ConstantConditions")
    default void putInto(ResultSet results, DataContainer<?> obj) throws SQLException {
        switch (getHeldType().getName()) {
            case "char":
            case "int":
                results.updateInt(getFieldName(), Polyfill.uncheckedCast(obj.getInputReference(getFieldName(), false).getValue().get(0)));
                break;
            case "double":
                results.updateDouble(getFieldName(), Polyfill.uncheckedCast(obj.getInputReference(getFieldName(), false).getValue().get(0)));
                break;
            case "float":
                results.updateFloat(getFieldName(), Polyfill.uncheckedCast(obj.getInputReference(getFieldName(), false).getValue().get(0)));
                break;
            case "long":
                results.updateLong(getFieldName(), Polyfill.uncheckedCast(obj.getInputReference(getFieldName(), false).getValue().get(0)));
                break;
            case "short":
                results.updateShort(getFieldName(), Polyfill.uncheckedCast(obj.getInputReference(getFieldName(), false).getValue().get(0)));
                break;
            case "String":
                results.updateString(getFieldName(), Polyfill.uncheckedCast(obj.getInputReference(getFieldName(), false).getValue().get(0)));
                break;
            case "boolean":
                results.updateBoolean(getFieldName(), Polyfill.uncheckedCast(obj.getInputReference(getFieldName(), false).getValue().get(0)));
                break;
            default:
                throw new UnsupportedOperationException("Unsupported type for extracting from ResultSet: " + getHeldType());
        }
    }

    @Experimental
    default FINAL getFrom(ResultSet results) throws SQLException {
        switch (getHeldType().getName()) {
            case "char":
            case "int":
                return Polyfill.uncheckedCast(results.getInt(getFieldName()));
            case "double":
                return Polyfill.uncheckedCast(results.getDouble(getFieldName()));
            case "float":
                return Polyfill.uncheckedCast(results.getFloat(getFieldName()));
            case "long":
                return Polyfill.uncheckedCast(results.getLong(getFieldName()));
            case "short":
                return Polyfill.uncheckedCast(results.getShort(getFieldName()));
            case "String":
                return Polyfill.uncheckedCast(results.getString(getFieldName()));
            case "boolean":
                return Polyfill.uncheckedCast(results.getBoolean(getFieldName()));
            default:
                throw new UnsupportedOperationException("Unsupported type for extracting from ResultSet: " + getHeldType());
        }
    }

    default FINAL getFrom(UniObjectNode node) {
        return getFrom(null, node);
    }

    default FINAL getFrom(SELF context, UniObjectNode node) {
        return process(context, extract(node));
    }

    default RefContainer<?, REMAP> remapAll(final SELF context, RefContainer<?, EXTR> from) {
        return from.map(each -> remap(context, each));
    }

    default FINAL process(SELF context, RefContainer<?, EXTR> from) {
        return finish(context, remapAll(context, from));
    }

    RefContainer<?, EXTR> extract(UniNode data);

    REMAP remap(SELF context, EXTR data);

    FINAL finish(SELF context, RefContainer<?, REMAP> parts);

    enum ExtractionMethod {
        VALUE, OBJECT, ARRAY
    }
}
