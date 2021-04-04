package org.comroid.uniform.adapter.xml.jsoup;

import org.comroid.uniform.adapter.AbstractSerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.impl.UniArrayNodeImpl;
import org.comroid.uniform.node.impl.UniObjectNodeImpl;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class JsoupXmlParser extends AbstractSerializationAdapter<Element, Element, Element> {
    public static final JsoupXmlParser instance = new JsoupXmlParser();
    private static final Supplier<Element> elementFactory = () -> new Element("data");

    private JsoupXmlParser() {
        super("application/xml", Element.class, elementFactory, Element.class, elementFactory);
    }

    @Override
    public DataStructureType<Element, ? extends Element, ? extends UniNode> typeOfData(String data) {
        return getObjectType();
    }

    @Override
    public UniObjectNode parse(@Nullable String data) {
        Document xml = parseXml(data);
        return createObjectNode(xml);
    }

    public Document parseXml(String data) {
        return Jsoup.parse(data, "", Parser.xmlParser());
    }

    @Override
    public UniObjectNode createObjectNode(Element node) {
        return new UniObjectNodeImpl(
                this,
                null,
                new JsoupObjectAdapter(node)
        );
    }

    @Override
    public UniArrayNode createArrayNode(Element node) {
        throw new UnsupportedOperationException("Only Object type is supported");
    }

    @Override
    public ValueAdapter<Object, Object> createValueAdapter(Object nodeBase, Predicate<Object> setter) {
        return new ValueAdapter<Object, Object>(nodeBase) {
            @Override
            public Object asActualType() {
                return base;
            }

            @Override
            protected boolean doSet(Object newValue) {
                return setter.test(newValue);
            }
        };
    }
}
