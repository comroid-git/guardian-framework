package org.comroid.test.model;

import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainerBase;

import static org.comroid.test.FastJSONLib.fastJsonLib;


@Location(Reaction.Binds.class)
public class Reaction extends DataContainerBase<DiscordAPI> {
    public Reaction(DiscordAPI dependencyObject, UniObjectNode data) {
        super(data, dependencyObject);
    }

    public interface Binds {
        @RootBind
        GroupBind<Reaction, DiscordAPI> GROUP = new GroupBind<>(fastJsonLib, "reaction", Reaction.class);
    }
}
