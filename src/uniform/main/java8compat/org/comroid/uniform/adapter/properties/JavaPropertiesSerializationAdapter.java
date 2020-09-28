package org.comroid.uniform.adapter.properties;

import org.comroid.annotations.Instance;
import org.comroid.uniform.DataStructureType;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

public class JavaPropertiesSerializationAdapter extends SerializationAdapter<Properties, Properties, Properties> {
    public static final @Instance
    JavaPropertiesSerializationAdapter JavaPropertiesAdapter
            = new JavaPropertiesSerializationAdapter();

    private JavaPropertiesSerializationAdapter() {
        super("text/x-java-propertie", Properties.class, null);
    }

    @Override
    public DataStructureType<SerializationAdapter<Properties, Properties, Properties>, Properties, ? extends Properties> typeOfData(String data) {
        final Properties properties = ofString(data);

        if (properties.keySet().stream()
                .map(String::valueOf)
                .allMatch(x -> x.matches("\\d+")))
            return arrayType;
        return objectType;
    }

    @Override
    public UniNode parse(@Nullable String data) {
        return createUniObjectNode(ofString(data));
    }

    @Override
    public UniObjectNode createUniObjectNode(Properties node) {
        return new UniObjectNode(this, new PropertiesAdapter(node));
    }

    @Override
    public UniArrayNode createUniArrayNode(Properties node) {
        throw new UnsupportedOperationException("Cannot create ArrayNode for Properties");
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
