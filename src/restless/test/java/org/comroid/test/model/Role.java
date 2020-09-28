package org.comroid.test.model;

import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainerBase;

import static org.comroid.test.FastJSONLib.fastJsonLib;


@Location(Role.Binds.class)
public class Role extends DataContainerBase<DiscordAPI> {
    public Role(DiscordAPI dependencyObject, UniObjectNode data) {
        super(data, dependencyObject);
    }

    public interface Binds {
        @RootBind
        GroupBind<Role, DiscordAPI> GROUP = new GroupBind<>(fastJsonLib, "role", Role.class);
    }
}
