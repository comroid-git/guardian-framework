package org.comroid.restless.endpoint;

import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainer;

public interface TypeBoundEndpoint<T extends DataContainer<? super T>> extends AccessibleEndpoint {
    GroupBind<T> getBoundType();
}
