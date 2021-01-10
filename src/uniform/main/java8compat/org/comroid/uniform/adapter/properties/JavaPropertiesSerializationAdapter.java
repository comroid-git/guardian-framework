package org.comroid.uniform.adapter.properties;

import org.comroid.annotations.Instance;
import org.comroid.api.Polyfill;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.adapter.AbstractSerializationAdapter;
import org.comroid.uniform.model.DataStructureType;
import org.comroid.uniform.model.ValueAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.impl.UniObjectNodeImpl;
import org.comroid.util.MapUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;
import java.util.function.Predicate;

public final class JavaPropertiesSerializationAdapter extends AbstractSerializationAdapter<Properties, Properties, Properties> {
    public static final @Instance
    JavaPropertiesSerializationAdapter JavaPropertiesAdapter
            = new JavaPropertiesSerializationAdapter();

    private JavaPropertiesSerializationAdapter() {
        super("text/x-java-propertie", Properties.class, null);
    }

    @Override
    public DataStructureType<Properties, ? extends Properties, ? extends UniNode> typeOfData(String data) {
        final Properties properties = ofString(data);

        if (properties.keySet().stream()
                .map(String::valueOf)
                .allMatch(x -> x.matches("\\d+")))
            return getArrayType();
        return getObjectType();
    }

    @Override
    public UniNode parse(@Nullable String data) {
        return createUniObjectNode(ofString(data));
    }

    @Override
    public UniObjectNode createUniObjectNode(Properties node) {
        return new UniObjectNodeImpl(this, MapUtil.hashtable(Polyfill.uncheckedCast(node)));
    }

    @Override
    public UniArrayNode createUniArrayNode(Properties node) {
        throw new UnsupportedOperationException("Cannot create ArrayNode for Properties");
    }

    @Override
    public ValueAdapter<Object, Object> createValueAdapter(Object nodeBase, final Predicate<Object> setter) {
        return new ValueAdapter<Object, Object>(nodeBase) {
            @Override
            public Object asActualType() {
                return base;
            }

            @Override
            protected boolean doSet(Object newValue) {
                return false;
            }
        };
    }

    private Properties ofString(String data) {
        try {
            final Properties prop = new Properties();
            prop.load(new StringReader(data));

            return prop;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
