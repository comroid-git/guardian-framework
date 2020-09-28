package org.comroid.test.model;

import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainerBase;

import static org.comroid.test.FastJSONLib.fastJsonLib;

@Location(Embed.Binds.class)
public class Embed extends DataContainerBase<DiscordAPI> {
    public Embed(DiscordAPI dependencyObject, UniObjectNode data) {
        super(data, dependencyObject);
    }

    public interface Binds {
        @RootBind
        GroupBind<Embed, DiscordAPI> GROUP = new GroupBind<>(fastJsonLib, "embed", Embed.class);
    }
}
