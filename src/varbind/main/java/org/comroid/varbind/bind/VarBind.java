package org.comroid.varbind.bind;

import org.comroid.varbind.container.DataContainer;
import org.comroid.varbind.multipart.PartialBind;

public interface VarBind<MEMBEROF extends DataContainer<? super MEMBEROF>, EXTR, REMAP, FINAL> extends
        PartialBind.Base<MEMBEROF, EXTR, REMAP, FINAL>,
        PartialBind.Grouped<MEMBEROF>,
        PartialBind.Extractor<EXTR>,
        PartialBind.Remapper<MEMBEROF, EXTR, REMAP>,
        PartialBind.Finisher<REMAP, FINAL> {
}
