package org.comroid.uniform.adapter.xml.jackson;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.comroid.annotations.Instance;
import org.comroid.uniform.adapter.model.JacksonAdapter;

public final class JacksonXMLAdapter extends JacksonAdapter {
    @Instance
    public static final JacksonXMLAdapter instance = new JacksonXMLAdapter();

    private JacksonXMLAdapter() {
        super("application/xml", new XmlMapper());
    }
}
