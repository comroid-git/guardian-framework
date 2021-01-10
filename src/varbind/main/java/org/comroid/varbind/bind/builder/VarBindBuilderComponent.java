package org.comroid.varbind.bind.builder;

import org.comroid.api.Builder;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

public abstract class VarBindBuilderComponent<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL>
        implements Builder<VarBind<SELF, EXTR, REMAP, FINAL>> {
    protected final GroupBind<SELF> group;
    protected final String fieldName;

    public GroupBind<SELF> getGroupBind() {
        return group;
    }

    public String getFieldName() {
        return fieldName;
    }

    protected VarBindBuilderComponent(GroupBind<SELF> group, String fieldName) {
        this.group = group;
        this.fieldName = fieldName;
    }

    @Override
    public VarBind<SELF, EXTR, REMAP, FINAL> build() {
        throw new AbstractMethodError();
    }
}
