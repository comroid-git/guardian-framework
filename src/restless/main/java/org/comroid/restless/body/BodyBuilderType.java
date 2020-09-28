package org.comroid.restless.body;

import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;

import java.util.function.Function;

public final class BodyBuilderType<B extends UniNode> implements Function<SerializationAdapter<?, ?, ?>, B> {
    public static final BodyBuilderType<UniObjectNode> OBJECT
            = new BodyBuilderType<>(SerializationAdapter::createUniObjectNode);
    public static final BodyBuilderType<UniArrayNode> ARRAY
            = new BodyBuilderType<>(SerializationAdapter::createUniArrayNode);

    private final Function<SerializationAdapter<?, ?, ?>, B> underlying;

    private BodyBuilderType(Function<SerializationAdapter<?, ?, ?>, B> underlying) {
        this.underlying = underlying;
    }

    @Override
    public B apply(SerializationAdapter<?, ?, ?> lib) {
        return underlying.apply(lib);
    }
}
