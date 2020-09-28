package org.comroid.varbind.multipart;

import org.comroid.api.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.api.UUIDContainer;
import org.comroid.mutatio.span.Span;
import org.comroid.spellbind.model.TypeFragmentProvider;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;

import java.io.Serializable;

public final class ExtractingBind {
    public static <E extends Serializable> TypeFragmentProvider<PartialBind.Extractor<E>> valueTypeExtractingProvider() {
        return new FragmentProviders.ToValueType<>();
    }

    public static TypeFragmentProvider<PartialBind.Extractor<UniObjectNode>> objectExtractingProvider() {
        return new FragmentProviders.ToUniObject();
    }

    public static TypeFragmentProvider<PartialBind.Extractor<UniArrayNode>> arrayExtractingProvider() {
        return new FragmentProviders.ToUniArray();
    }

    public static final class ToValueType<E extends Serializable> extends UUIDContainer.Base implements PartialBind.Extractor<E> {
        private static final Invocable<? super ToValueType<?>> constructor = Invocable.ofConstructor(ToValueType.class);

        private final ValueType<E> valueType;

        public ToValueType(ValueType<E> valueType) {
            this.valueType = valueType;
        }

        @Override
        public Span<E> extract(UniNode from) {
            final String fieldName = as(PartialBind.Base.class, "Missing Base attribute").getFieldName();

            return as(PartialBind.Finisher.class)
                    .map(bind -> {
                        final UniNode target = from.get(fieldName);
                        if (bind.isListing())
                            return target
                                    .asNodeList()
                                    .stream()
                                    .map(node -> node.as(valueType))
                                    .collect(Span.collector());
                        else {
                            final E as = target.as(valueType);
                            return Span.immutable(as);
                        }
                    }).orElseThrow(() -> new AssertionError("Missing Finisher attribute"));
        }
    }

    public static final class ToUniObject extends UUIDContainer.Base implements PartialBind.Extractor<UniObjectNode> {
        private static final Invocable<? super ToUniObject> constructor = Invocable.ofConstructor(ToUniObject.class);

        @Override
        public Span<UniObjectNode> extract(UniNode from) {
            return as(PartialBind.Base.class)
                    .map(PartialBind.Base::getFieldName)
                    .map(str -> {
                        final UniNode node = from.get(str);

                        if (node.isArrayNode())
                            return node.asNodeList()
                                    .stream()
                                    .map(UniNode::asObjectNode)
                                    .collect(Span.collector());
                        else return Span.immutable(node.asObjectNode());
                    })
                    .orElseThrow(() -> new AssertionError("Missing Base attribute"));
        }
    }

    public static final class ToUniArray extends UUIDContainer.Base implements PartialBind.Extractor<UniArrayNode> {
        private static final Invocable<? super ToUniArray> constructor = Invocable.ofConstructor(ToUniArray.class);

        @Override
        public Span<UniArrayNode> extract(UniNode from) {
            return as(PartialBind.Base.class)
                    .map(PartialBind.Base::getFieldName)
                    .map(str -> {
                        final UniNode node = from.get(str);

                        if (node.isArrayNode())
                            return node.asNodeList()
                                    .stream()
                                    .map(UniNode::asArrayNode)
                                    .collect(Span.collector());
                        else return Span.immutable(node.asArrayNode());
                    })
                    .orElseThrow(() -> new AssertionError("Missing Base attribute"));
        }
    }

    private static final class FragmentProviders {
        private interface ExtractorProvider<E> extends TypeFragmentProvider<PartialBind.Extractor<E>> {
            @Override
            default Class<PartialBind.Extractor<E>> getInterface() {
                return Polyfill.uncheckedCast(PartialBind.Extractor.class);
            }
        }

        private static final class ToValueType<E> implements ExtractorProvider<E> {
            @Override
            public Invocable.TypeMap<? extends PartialBind.Extractor<E>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(ExtractingBind.ToValueType.constructor.typeMapped());
            }
        }

        private static final class ToUniObject implements ExtractorProvider<UniObjectNode> {
            @Override
            public Invocable.TypeMap<? extends PartialBind.Extractor<UniObjectNode>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(ExtractingBind.ToUniObject.constructor.typeMapped());
            }
        }

        private static final class ToUniArray implements ExtractorProvider<UniArrayNode> {
            @Override
            public Invocable.TypeMap<? extends PartialBind.Extractor<UniArrayNode>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(ExtractingBind.ToUniArray.constructor.typeMapped());
            }
        }
    }
}
