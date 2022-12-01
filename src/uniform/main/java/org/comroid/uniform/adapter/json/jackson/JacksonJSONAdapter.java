package org.comroid.uniform.adapter.json.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.comroid.annotations.Instance;
import org.comroid.uniform.adapter.model.JacksonAdapter;

public final class JacksonJSONAdapter extends JacksonAdapter {
    @Instance
    public static final JacksonJSONAdapter instance = new JacksonJSONAdapter();

    private JacksonJSONAdapter() {
        super("application/json", new ObjectMapper());
    }
}
