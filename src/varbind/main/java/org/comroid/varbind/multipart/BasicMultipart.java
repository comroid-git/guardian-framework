package org.comroid.varbind.multipart;

import org.comroid.api.HeldType;
import org.comroid.api.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.api.UUIDContainer;
import org.comroid.spellbind.model.TypeFragmentProvider;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.container.DataContainer;

public final class BasicMultipart {
    public static <SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL> TypeFragmentProvider<PartialBind.Base<SELF, EXTR, REMAP, FINAL>> baseProvider() {
        return new FragmentProviders.Base<>();
    }

    public static <SELF extends DataContainer<? super SELF>> TypeFragmentProvider<PartialBind.Grouped<SELF>> groupedProvider() {
        return new FragmentProviders.Grouped<>();
    }

    public static final class Base<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL> extends UUIDContainer.Base implements PartialBind.Base<SELF, EXTR, REMAP, FINAL> {
        private static final Invocable<? super BasicMultipart.Base> constructor = Invocable.ofConstructor(BasicMultipart.Base.class);
        private final String fieldName;
        private final HeldType<EXTR> heldType;
        private final boolean required;

        @Override
        public String getFieldName() {
            return fieldName;
        }

        @Override
        public HeldType<EXTR> getHeldType() {
            return heldType;
        }

        @Override
        public boolean isRequired() {
            return required;
        }

        public Base(String fieldName, HeldType<EXTR> heldType, boolean required) {
            this.fieldName = fieldName;
            this.heldType = heldType;
            this.required = required;
        }

        @Override
        public String toString() {
            return String.format("VarBind %s%s {heldType=%s, required=%s}",
                    as(Grouped.class).map(g -> g.group.getName() + '.').orElse(""),
                    fieldName,
                    heldType,
                    required);
        }
    }

    public static final class Grouped<SELF extends DataContainer<? super SELF>> extends UUIDContainer.Base implements PartialBind.Grouped<SELF> {
        private static final Invocable<? super Grouped<?>> constructor = Invocable.ofConstructor(Grouped.class);
        private final GroupBind<SELF> group;

        @Override
        public GroupBind<SELF> getGroup() {
            return group;
        }

        public Grouped(GroupBind<SELF> group) {
            this.group = group;
        }
    }

    private static final class FragmentProviders {
        private static final class Base<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL> implements TypeFragmentProvider<PartialBind.Base<SELF, EXTR, REMAP, FINAL>> {
            @Override
            public Class<PartialBind.Base<SELF, EXTR, REMAP, FINAL>> getInterface() {
                return Polyfill.uncheckedCast(PartialBind.Base.class);
            }

            @Override
            public Invocable.TypeMap<? extends PartialBind.Base<SELF, EXTR, REMAP, FINAL>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(BasicMultipart.Base.constructor.typeMapped());
            }
        }

        private static final class Grouped<SELF extends DataContainer<? super SELF>> implements TypeFragmentProvider<PartialBind.Grouped<SELF>> {
            @Override
            public Class<PartialBind.Grouped<SELF>> getInterface() {
                return Polyfill.uncheckedCast(PartialBind.Grouped.class);
            }

            @Override
            public Invocable.TypeMap<? extends PartialBind.Grouped<SELF>> getInstanceSupplier() {
                return Polyfill.uncheckedCast(BasicMultipart.Grouped.constructor.typeMapped());
            }
        }
    }
}
