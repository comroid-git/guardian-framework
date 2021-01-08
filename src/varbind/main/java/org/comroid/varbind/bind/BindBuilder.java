package org.comroid.varbind.bind;

import org.comroid.api.Builder;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.Contract;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public interface BindBuilder<SELF extends DataContainer<? super SELF>, EXTR, REMAP, FINAL> extends Builder<VarBind<SELF, EXTR, REMAP, FINAL>> {
    GroupBind<SELF> getGroupBind();

    String getFieldName();

    boolean isRequired();

    @Contract(value = "_ -> this", mutates = "this")
    BindBuilder<SELF, EXTR, REMAP, FINAL> setRequired(boolean required);

    @Contract(value = "_ -> this", mutates = "this")
    BindBuilder<SELF, EXTR, REMAP, FINAL> setClassLoader(ClassLoader classLoader);

    @Contract(value = "-> this", mutates = "this")
    BindBuilder<SELF, EXTR, REMAP, FINAL> setRequired();

    @Contract(value = "-> this", mutates = "this")
    BindBuilder<SELF, EXTR, REMAP, FINAL> setOptional();

    @Contract(value = "_ -> this", mutates = "this")
    <E extends Serializable> BindBuilder<SELF, E, REMAP, FINAL> extractAs(ValueType<E> valueType);

    @Contract(value = "-> this", mutates = "this")
    BindBuilder<SELF, UniObjectNode, REMAP, FINAL> extractAsObject();

    @Contract(value = "-> this", mutates = "this")
    BindBuilder<SELF, Span<UniNode>, REMAP, FINAL> extractAsArray();

    BindBuilder<SELF, EXTR, EXTR, FINAL> asIdentities();

    @Contract(value = "_ -> this", mutates = "this")
    <R> BindBuilder<SELF, EXTR, R, FINAL> andRemap(Function<EXTR, R> remapper);

    @Contract(value = "_ -> this", mutates = "this")
    <R> BindBuilder<SELF, EXTR, R, FINAL> andResolve(BiFunction<SELF, EXTR, R> resolver);

    @Contract(value = "_ -> this", mutates = "this")
    <R extends DataContainer<? super R>> BindBuilder<SELF, UniObjectNode, R, FINAL> andConstruct(GroupBind<R> targetBind);

    @Contract(value = "_,_,_ -> this", mutates = "this")
    <R extends DataContainer<? super R>, ID> BindBuilder<SELF, UniObjectNode, R, FINAL> andProvide(
            VarBind<? super R, ?, ?, ID> idBind,
            BiFunction<SELF, ID, R> resolver,
            GroupBind<R> targetBind
    );

    @Contract(value = "_,_,_ -> this", mutates = "this")
    <R extends DataContainer<? super R>, ID> BindBuilder<SELF, UniObjectNode, R, FINAL> andProvideRef(
            VarBind<? super R, ?, ?, ID> idBind,
            BiFunction<SELF, ID, Reference<R>> resolver,
            GroupBind<R> targetBind
    );

    @Contract(value = "-> this", mutates = "this")
    BindBuilder<SELF, EXTR, REMAP, REMAP> onceEach();

    @Contract(value = "_ -> this", mutates = "this")
    <C extends Collection<REMAP>> BindBuilder<SELF, EXTR, REMAP, C> intoCollection(Supplier<C> collectionProvider);

    @Override
    VarBind<SELF, EXTR, REMAP, FINAL> build();
}
