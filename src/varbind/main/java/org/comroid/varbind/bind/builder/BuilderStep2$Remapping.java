package org.comroid.varbind.bind.builder;

import org.comroid.api.*;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.uniform.node.UniValueNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BuilderStep2$Remapping<SELF extends DataContainer<? super SELF>, EXTR>
        extends VarBindBuilderComponent<SELF, EXTR, Void, Void>
        implements Builder<VarBind<SELF, EXTR, EXTR, EXTR>> {
    private final ValueType<EXTR> valueType;
    private final VarBind.ExtractionMethod extractionMethod;

    BuilderStep2$Remapping(
            GroupBind<SELF> group,
            String fieldName,
            ValueType<EXTR> valueType,
            VarBind.ExtractionMethod extractionMethod
    ) {
        super(group, fieldName);

        this.valueType = valueType;
        this.extractionMethod = extractionMethod;
    }

    public BuilderStep3$Finishing<SELF, EXTR, EXTR> asIdentities() {
        return andRemap(Function.identity());
    }

    public <R> BuilderStep3$Finishing<SELF, EXTR, R> andRemap(
            final Function<? super EXTR, ? extends R> remapper) {
        return andResolve((self, extr) -> {
            if (extr instanceof UniValueNode)
                return remapper.apply(((UniValueNode) extr).as(valueType));
            else return remapper.apply(extr);
        });
    }

    public <R> BuilderStep3$Finishing<SELF, EXTR, R> andResolve(
            final BiFunction<? super SELF, ? super EXTR, ? extends R> resolver) {
        return new BuilderStep3$Finishing<>(group, fieldName, valueType, extractionMethod, resolver);
    }

    public <R> BuilderStep3$Finishing<SELF, EXTR, R> andRemapRef(
            Function<? super EXTR, ? extends Rewrapper<? extends R>> remapper
    ) {
        return andRemap(remapper.andThen(Rewrapper::get));
    }

    public <R> BuilderStep3$Finishing<SELF, EXTR, R> andResolveRef(
            BiFunction<? super SELF, ? super EXTR, ? extends Rewrapper<? extends R>> resolver) {
        return andResolve(resolver.andThen(Rewrapper::get));
    }

    public <ID, R extends DataContainer<? super R>> BuilderStep3$Finishing<SELF, UniObjectNode, R> andProvide(
            VarBind<?, ?, ?, ID> identification,
            BiFunction<? super SELF, ? super ID, ? extends R> resolver,
            BiFunction<? super SELF, UniObjectNode, ? extends R> creator
    ) {
        return Polyfill.uncheckedCast(andResolve(Polyfill.<BiFunction<? super SELF, ? super EXTR, ? extends R>>
                uncheckedCast((BiFunction<SELF, UniObjectNode, R>) (ctx, obj) -> {
            ID id = identification.getFrom(obj);
            R result = resolver.apply(ctx, id);
            if (result != null)
                return result;
            return creator.apply(ctx, obj);
        })));
    }

    public <ID, R extends DataContainer<? super R>> BuilderStep3$Finishing<SELF, UniObjectNode, R> andProvideRef(
            VarBind<?, ?, ?, ID> identification,
            BiFunction<? super SELF, ? super ID, ? extends Rewrapper<? extends R>> resolver,
            BiFunction<? super SELF, UniObjectNode, ? extends R> creator
    ) {
        return andProvide(identification, resolver.andThen(Rewrapper::get), creator);
    }

    public <R extends DataContainer<? super R>> BuilderStep3$Finishing<SELF, UniObjectNode, R> andConstruct(
            GroupBind<R> type
    ) {
        return Polyfill.uncheckedCast(andResolve(
                Polyfill.uncheckedCast(type.getResolver()
                        .assertion("No resolver present"))));
    }

    @Override
    public VarBind<SELF, EXTR, EXTR, EXTR> build() {
        return asIdentities().build();
    }
}
