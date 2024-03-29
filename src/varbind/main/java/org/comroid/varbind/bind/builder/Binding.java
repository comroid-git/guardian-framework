package org.comroid.varbind.bind.builder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.Polyfill;
import org.comroid.api.ValueType;
import org.comroid.mutatio.model.RefContainer;
import org.comroid.mutatio.ref.ReferenceList;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StandardValueType;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class Binding<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        implements VarBind<SELF, EXTR, REMAP, FINAL> {
    private final static Logger logger = LogManager.getLogger();
    private final GroupBind<SELF> group;
    private final String fieldName;
    private final boolean required;
    private final boolean ignoreInDB;
    private final boolean identifier;
    private final ValueType<EXTR> valueType;
    private final ExtractionMethod extractionMethod;
    private final BiFunction<? super SELF, ? super EXTR, ? extends REMAP> remapper;
    private final Function<? super RefContainer<?, REMAP>, ? extends FINAL> finisher;
    private final Function<? super SELF, ? extends FINAL> defaultSupplier;
    private final Set<VarBind<? extends SELF, ?, ?, ?>> dependencies;

    @Override
    public ValueType<EXTR> getHeldType() {
        return valueType;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public Set<VarBind<? extends SELF, ?, ?, ?>> getDependencies() {
        return dependencies;
    }

    @Override
    public GroupBind<SELF> getGroup() {
        return group;
    }

    @Override
    public boolean isListing() {
        return extractionMethod == ExtractionMethod.ARRAY;
    }

    Binding(
            GroupBind<SELF> group,
            String fieldName,
            boolean required,
            boolean ignoreInDB,
            boolean identifier,
            ValueType<EXTR> valueType,
            ExtractionMethod extractionMethod,
            BiFunction<? super SELF, ? super EXTR, ? extends REMAP> remapper,
            Function<? super RefContainer<?, REMAP>, ? extends FINAL> finisher,
            Function<? super SELF, ? extends FINAL> defaultSupplier,
            Set<VarBind<? extends SELF, ?, ?, ?>> dependencies
    ) {
        this.group = group;
        this.fieldName = fieldName;
        this.required = required;
        this.ignoreInDB = ignoreInDB;
        this.identifier = identifier;
        this.valueType = valueType;
        this.extractionMethod = extractionMethod;
        this.remapper = remapper;
        this.finisher = finisher;
        this.defaultSupplier = defaultSupplier;
        this.dependencies = dependencies;
    }

    @Override
    public boolean ignoreInDB() {
        return ignoreInDB;
    }

    @Override
    public boolean identifier() {
        return identifier;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Binding<?, ?, ?, ?> binding = (Binding<?, ?, ?, ?>) o;
        return group.equals(binding.group) && fieldName.equals(binding.fieldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(group, fieldName);
    }

    @Override
    public String toString() {
        return getAlternateName();
    }

    @Override
    public RefContainer<?, EXTR> extract(UniNode from) {
        try {
            final UniNode target = getTargetNode(from);

            switch (extractionMethod) {
                case VALUE:
                    assert valueType instanceof StandardValueType;
                    EXTR value = target.as(valueType);
                    return Span.immutable(value);
                case OBJECT:
                    assert group.getFromContext().getObjectType().equals(valueType);
                    UniObjectNode obj = target.asObjectNode();
                    EXTR cast = Polyfill.uncheckedCast(obj);
                    return Span.immutable(cast);
                case ARRAY:
                    if (valueType instanceof StandardValueType) {
                        // extract values array
                        return target.streamNodes()
                                .map(each -> each.as(valueType))
                                .collect(Span.collector());
                    } else if (group.getFromContext().getArrayType().equals(valueType)) {
                        // extract uninode array
                        return target.streamNodes()
                                .map(UniNode::asObjectNode)
                                // assume EXTR = UniObjectNode !!
                                .map(Polyfill::<EXTR>uncheckedCast)
                                .collect(Span.collector());
                    }
            }
            throw new AssertionError("unreachable");
        } catch (Exception e) {
            logger.error("Could not extract data for bind " + fieldName, e);
            return ReferenceList.empty();
        }
    }

    private UniNode getTargetNode(UniNode from) {
        if (fieldName.isEmpty())
            return from;
        final String[] split = fieldName.split("\\.");
        if (split.length == 0)
            throw new AssertionError();
        if (split.length == 1)
            return from.get(split[0]);
        return unwrapNames(from, split, 0);
    }

    private UniNode unwrapNames(UniNode from, String[] names, int index) {
        if (index >= names.length)
            return from;
        return unwrapNames(from.get(names[index]), names, index + 1);
    }

    @Override
    public REMAP remap(SELF context, EXTR data) {
        try {
            return remapper.apply(context, data);
        } catch (Exception e) {
            logger.error("Could not remap data for bind " + fieldName, e);
            return null;
        }
    }

    @Override
    public FINAL finish(SELF context, RefContainer<?, REMAP> parts) {
        try {
            if ((parts == null || parts.size() == 0) && defaultSupplier != null)
                return defaultSupplier.apply(context);
            return finisher.apply(parts);
        } catch (Exception e) {
            logger.error("Could not finalize data for bind " + fieldName, e);
            return null;
        }
    }
}
