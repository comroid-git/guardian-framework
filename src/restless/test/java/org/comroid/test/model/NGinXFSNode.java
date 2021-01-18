package org.comroid.test.model;

import org.comroid.uniform.node.impl.StandardValueType;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainerBase;

@Location(NGinXFSNode.Bind.class)
public final class NGinXFSNode extends DataContainerBase<Void> {
    public String getName() {
        return get(Bind.Name);
    }

    public String getType() {
        return get(Bind.Type);
    }

    public String getMtime() {
        return get(Bind.MTime);
    }

    protected NGinXFSNode(UniObjectNode initialData) {
        super(context, initialData);
    }

    interface Bind {
        @RootBind
        GroupBind<NGinXFSNode, Void> ROOT
                = new GroupBind<>(FastJSONLib.fastJsonLib, "fsnode", NGinXFSNode.class);
        VarBind<Object, String, String, String> Name = ROOT.createBind("name")
                .extractAs(StandardValueType.STRING)
                .asIdentities()
                .onceEach()
                .build();
        VarBind<Object, String, String, String> Type = ROOT.createBind("type")
                .extractAs(StandardValueType.STRING)
                .asIdentities()
                .onceEach()
                .build();
        VarBind<Object, String, String, String> MTime = ROOT.createBind("mtime")
                .extractAs(StandardValueType.STRING)
                .asIdentities()
                .onceEach()
                .build();
    }
}
