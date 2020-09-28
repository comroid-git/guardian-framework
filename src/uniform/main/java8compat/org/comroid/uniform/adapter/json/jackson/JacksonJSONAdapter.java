package org.comroid.uniform.adapter.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.comroid.annotations.Instance;
import org.comroid.uniform.adapter.model.JacksonAdapter;

public class JacksonJSONAdapter extends JacksonAdapter {
    public static final @Instance JacksonAdapter instance = new JacksonJSONAdapter();

    private JacksonJSONAdapter() {
        super("application/json", new ObjectMapper());
    }
}
