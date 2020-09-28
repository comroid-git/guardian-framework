package org.comroid.test.model;

import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainerBase;

import static org.comroid.test.FastJSONLib.fastJsonLib;

@Location(User.Binds.class)
public class User extends DataContainerBase<DiscordAPI> {
    public User(DiscordAPI dependencyObject, UniObjectNode data) {
        super(data, dependencyObject);
    }

    public interface Binds {
        @RootBind
        GroupBind<User, DiscordAPI> GROUP = new GroupBind<>(fastJsonLib, "user", User.class);
    }
}
