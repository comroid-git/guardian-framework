package org.comroid.test.varbind;

import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainerBase;

@Location(Dummy.class)
public final class Dummy extends DataContainerBase<Dummy> {
    @RootBind
    public static final GroupBind group = new GroupBind(FastJSONLib.fastJsonLib, "dummy");

    public Dummy() {
        super(null);
    }

    public String modify(String str) {
        int mid = str.length() / 5;
        return str.substring(mid) + str.substring(0, mid);
    }
}
