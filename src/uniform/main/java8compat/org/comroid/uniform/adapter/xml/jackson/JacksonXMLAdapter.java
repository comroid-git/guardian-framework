package org.comroid.uniform.adapter.xml.jackson;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.comroid.uniform.adapter.model.jackson.JacksonAdapter;

public final class JacksonXMLAdapter extends JacksonAdapter {
    public static final JacksonXMLAdapter instance = new JacksonXMLAdapter();

    private JacksonXMLAdapter() {
        super("application/xml", new XmlMapper());
    }
}
