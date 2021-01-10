package org.comroid.varbind.bind;

import org.comroid.api.Builder;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface VarBindBuilder<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL> extends Builder<VarBind<SELF, EXTR, REMAP, FINAL>> {
    GroupBind<SELF> getGroupBind();

    String getFieldName();

    boolean isRequired();

    @Contract(value = "_ -> this", mutates = "this")
    VarBindBuilder<SELF, EXTR, REMAP, FINAL> setRequired(boolean required);

    @Contract(value = "-> this", mutates = "this")
    default VarBindBuilder<SELF, EXTR, REMAP, FINAL> setRequired() {
        return setRequired(true);
    }

    @Contract(value = "-> this", mutates = "this")
    default VarBindBuilder<SELF, EXTR, REMAP, FINAL> setOptional() {
        return setRequired(false);
    }

    @Contract(value = "_ -> this", mutates = "this")
    <E extends Serializable> VarBindBuilder<SELF, E, REMAP, FINAL> extractAs(ValueType<E> valueType);

    @Contract(value = "-> this", mutates = "this")
    VarBindBuilder<SELF, UniObjectNode, REMAP, FINAL> extractAsObject();

    @Contract(value = "-> this", mutates = "this")
    default VarBindBuilder<SELF, UniObjectNode, REMAP, FINAL> extractAsArray() {
        return extractAsArray(null);
    }

    @Contract(value = "_ -> this", mutates = "this")
    <R> VarBindBuilder<SELF, R, REMAP, FINAL> extractAsArray(@Nullable ValueType<R> valueType);

    VarBindBuilder<SELF, EXTR, EXTR, FINAL> asIdentities();

    @Contract(value = "_ -> this", mutates = "this")
    default <R> VarBindBuilder<SELF, EXTR, R, FINAL> andRemapRef(
            Function<? super EXTR, ? extends Rewrapper<? extends R>> remapper
    ) {
        return andRemap(remapper.andThen(Rewrapper::get));
    }

    @Contract(value = "_ -> this", mutates = "this")
    <R> VarBindBuilder<SELF, EXTR, R, FINAL> andRemap(Function<? super EXTR, ? extends R> remapper);

    @Contract(value = "_ -> this", mutates = "this")
    <R> VarBindBuilder<SELF, EXTR, R, FINAL> andResolve(
            BiFunction<? super SELF, ? super EXTR, ? extends R> resolver);

    @Contract(value = "_ -> this", mutates = "this")
    default <R> VarBindBuilder<SELF, EXTR, R, FINAL> andResolveRef(
            BiFunction<? super SELF, ? super EXTR, ? extends Rewrapper<? extends R>> resolver) {
        return andResolve(resolver.andThen(Rewrapper::get));
    }

    default <ID, R extends DataContainer<? super R>> VarBindBuilder<SELF, UniObjectNode, R, FINAL> andProvide(
            VarBind<?, ?, ?, ID> identification,
            final BiFunction<? super SELF, ? super ID, ? extends R> resolver,
            final GroupBind<R> targetType
    ) {
        return Polyfill.uncheckedCast(andResolve(Polyfill.<BiFunction<? super SELF, ? super EXTR, ? extends R>>
                uncheckedCast((BiFunction<SELF, UniObjectNode, R>) (ctx, obj) -> {
            ID id = identification.getFrom(obj);
            R result = resolver.apply(ctx, id);
            if (result != null)
                return result;
            return targetType.getConstructor()
                    .map(invoc -> invoc.autoInvoke(ctx, obj))
                    .orElseThrow(() -> new IllegalArgumentException(targetType + " has no available Constructor"));
        })));
    }

    default <ID, R extends DataContainer<? super R>> VarBindBuilder<SELF, UniObjectNode, R, FINAL> andProvideRef(
            VarBind<?, ?, ?, ID> identification,
            final BiFunction<? super SELF, ? super ID, ? extends Rewrapper<? extends R>> resolver,
            final GroupBind<R> targetType
    ) {
        return andProvide(identification, resolver.andThen(Rewrapper::get), targetType);
    }

    @Contract(value = "_ -> this", mutates = "this")
    default <R extends DataContainer<? super R>> VarBindBuilder<SELF, UniObjectNode, R, FINAL> andConstruct(
            GroupBind<R> targetBind) {
        return Polyfill.uncheckedCast(targetBind.getConstructor()
                .map(invoc -> andResolve(Polyfill.<BiFunction<? super SELF, ? super EXTR, ? extends R>>
                        uncheckedCast((BiFunction<SELF, UniObjectNode, R>) invoc::autoInvoke)))
                .orElseThrow(() -> new IllegalArgumentException(targetBind + " has no available Constructor")));
    }

    @Contract(value = "-> this", mutates = "this")
    VarBindBuilder<SELF, EXTR, REMAP, REMAP> onceEach();

    @Contract(value = "-> this", mutates = "this")
    default VarBindBuilder<SELF, EXTR, REMAP, Span<REMAP>> intoSpan() {
        //noinspection unchecked
        return reformatSpan((Function) Function.identity());
    }

    @Contract(value = "_ -> this", mutates = "this")
    default <C extends Collection<REMAP>> VarBindBuilder<SELF, EXTR, REMAP, C> intoCollection(
            Supplier<C> collectionSupplier) {
        return reformatSpan(span -> {
            C col = collectionSupplier.get();
            col.addAll(span);
            return Polyfill.uncheckedCast(col);
        });
    }

    @Contract(value = "_ -> this", mutates = "this")
    <C extends Collection<REMAP>> VarBindBuilder<SELF, EXTR, REMAP, C> reformatSpan(
            Function<? super Span<REMAP>, ? extends FINAL> spanResolver);

    @Override
    VarBind<SELF, EXTR, REMAP, FINAL> build();
}
