package org.comroid.varbind.bind;

import jdk.internal.reflect.CallerSensitive;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Invocable;
import org.comroid.api.Named;
import org.comroid.api.Polyfill;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.StackTraceUtils;
import org.comroid.varbind.bind.impl.BindingBuilder;
import org.comroid.varbind.container.DataContainer;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class GroupBind<T extends DataContainer<? super T>> implements Iterable<GroupBind<? extends T>>, Named, ContextualProvider.Member {
    final List<? extends VarBind<T, ?, ?, ?>> children = new ArrayList<>();
    private final SerializationAdapter<?, ?, ?> serializationAdapter;
    private final String groupName;
    private final Span<GroupBind<? super T>> parents;
    private final List<GroupBind<? extends T>> subgroups = new ArrayList<>();
    private final @Nullable Invocable<? extends T> constructor;

    public List<? extends VarBind<T, ?, ?, ?>> getDirectChildren() {
        return Collections.unmodifiableList(children);
    }

    public Optional<Invocable<? extends T>> getConstructor() {
        return Optional.ofNullable(constructor);
    }

    @Override
    public String getName() {
        return groupName;
    }

    public Span<GroupBind<? super T>> getParents() {
        return parents;
    }

    public List<GroupBind<? extends T>> getSubgroups() {
        return subgroups;
    }

    @Override
    public @NotNull SerializationAdapter<?, ?, ?> getFromContext() {
        return serializationAdapter;
    }

    public GroupBind(
            ContextualProvider context,
            String groupName
    ) {
        this(context, groupName, (Invocable<T>) null);
    }

    public GroupBind(
            ContextualProvider context,
            String groupName,
            Class<? extends T> constructorClass
    ) {
        this(context, groupName, Invocable.ofConstructor(constructorClass, UniObjectNode.class));
    }

    public GroupBind(
            ContextualProvider context,
            String groupName,
            Invocable<? extends T> invocable
    ) {
        this(Span.empty(), context, groupName, invocable);
    }

    private GroupBind(
            GroupBind<? super T> parent,
            ContextualProvider context,
            String groupName,
            @Nullable Invocable<? extends T> invocable
    ) {
        this(
                Span.singleton(Objects.requireNonNull(parent, "parents")),
                context,
                groupName,
                invocable
        );
    }

    private GroupBind(
            Span<GroupBind<? super T>> parents,
            ContextualProvider context,
            String groupName,
            @Nullable Invocable<? extends T> invocable
    ) {
        this.parents = parents;
        this.serializationAdapter = context.requireFromContext(SerializationAdapter.class);
        this.groupName = groupName;
        this.constructor = invocable;
    }

    private static <T extends DataContainer<? extends D>, D> GroupBind<? super T> findRootParent(
            Collection<GroupBind<? super T>> groups
    ) {
        if (groups.size() == 0)
            throw new AssertionError();

        if (groups.size() == 1)
            return groups.iterator().next();

        //noinspection RedundantCast -> false positive; todo: wtf is this
        return (GroupBind<? super T>) findRootParent(groups.stream()
                .map(GroupBind::getParents)
                .flatMap(Collection::stream)
                .map(it -> (GroupBind<? super T>) it)
                .collect(Collectors.toSet()));
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataContainer<? super T>> GroupBind<T> combine(
            String groupName,
            GroupBind<? super T>... parents
    ) {
        return combine(groupName, null, parents);
    }

    @SuppressWarnings("unchecked")
    public static <T extends DataContainer<? super T>> GroupBind<T> combine(
            String groupName,
            Invocable<? extends T> invocable,
            GroupBind<? super T>... parents
    ) {
        final GroupBind<? super T> rootParent = (GroupBind<? super T>) findRootParent(Polyfill.uncheckedCast(Arrays.asList(parents)));

        return new GroupBind<>(
                Span.immutable(parents),
                rootParent.serializationAdapter,
                groupName,
                invocable
        );
    }

    @Override
    public String toString() {
        return String.format("GroupBind<%s>", nameStringRecursive(getParents(), getName()));
    }

    @SuppressWarnings({"unchecked", "InfiniteRecursion", "ConstantConditions"})
    private static String nameStringRecursive(Span<? extends GroupBind<?>> parents, String ownName) {
        return (!parents.isEmpty()
                ? nameStringRecursive(
                parents.stream().flatMap(groupBind -> groupBind.getParents().stream()).collect(Span.collector()),
                parents.size() == 1
                        ? parents.get(0).getName()
                        : parents.stream()
                        .map(GroupBind::getName)
                        .collect(Collectors.joining(";", "[", "]"))) + '.'
                : "") + ownName;
    }

    public Optional<GroupBind<? extends T>> findGroupForData(UniObjectNode data) {
        if (isValidData(data)) {
            if (subgroups.isEmpty())
                return Optional.of(this);

            //noinspection rawtypes
            GroupBind[] fitting = subgroups.stream()
                    .filter(group -> group.isValidData(data))
                    .toArray(GroupBind[]::new);
            if (fitting.length == 1)
                //noinspection unchecked
                return (Optional<GroupBind<? extends T>>) fitting[0].findGroupForData(data);

            throw new UnsupportedOperationException(String.format(
                    "%s fitting subgroups found: %s",
                    (fitting.length == 0 ? "No" : "Too many"),
                    Arrays.toString(fitting)
            ));
        } else return Optional.empty();
    }

    public boolean isValidData(UniObjectNode data) {
        return streamAllChildren().allMatch(bind -> data.has(bind.getFieldName()) || !bind.isRequired());
    }

    public Stream<? extends VarBind<? super T, ?, ?, ?>> streamAllChildren() {
        return Stream.concat(
                getParents().stream()
                        .flatMap(GroupBind::streamAllChildren),
                children.stream())
                .map(Polyfill::<VarBind<? super T, ?, ?, ?>>uncheckedCast)
                .distinct();
    }

    public Invocable<? super T> autoConstructor(
            Class<T> resultType
    ) {
        final Class<?>[] typesUnordered = {
                UniObjectNode.class, SerializationAdapter.class, serializationAdapter.objectType.typeClass()
        };

        return Invocable.ofConstructor(resultType, typesUnordered);
    }

    @CallerSensitive
    public <R extends T> GroupBind<R> rootGroup(String subGroupName) {
        return subGroup(this, subGroupName, Polyfill.<Class<R>>uncheckedCast(StackTraceUtils.callerClass(1)));
    }

    @CallerSensitive
    public <R extends T> GroupBind<R> rootGroup(GroupBind parent, String subGroupName) {
        return subGroup(parent, subGroupName, Polyfill.<Class<R>>uncheckedCast(StackTraceUtils.callerClass(1)));
    }

    public <R extends T> GroupBind<R> subGroup(String subGroupName) {
        return subGroup(this, subGroupName);
    }

    public <R extends T> GroupBind<R> subGroup(GroupBind parent, String subGroupName) {
        return subGroup(parent, subGroupName, (Invocable<R>) null);
    }

    public <R extends T> GroupBind<R> subGroup(String subGroupName, Class<? extends T> type) {
        return subGroup(this, subGroupName, type);
    }

    public <R extends T> GroupBind<R> subGroup(GroupBind parent, String subGroupName, Class<? extends T> type) {
        return subGroup(parent, subGroupName, Polyfill.<Invocable<R>>uncheckedCast(Invocable.ofClass(type)));
    }

    public <R extends T> GroupBind<R> subGroup(String subGroupName, Constructor<? extends T> type) {
        return subGroup(this, subGroupName, type);
    }

    public <R extends T> GroupBind<R> subGroup(GroupBind parent, String subGroupName, Constructor<? extends T> type) {
        return subGroup(parent, subGroupName, Polyfill.<Invocable<R>>uncheckedCast(Invocable.ofConstructor(type)));
    }

    public <R extends T> GroupBind<R> subGroup(String subGroupName, Invocable<? extends R> constructor) {
        return subGroup(this, subGroupName, constructor);
    }

    public <R extends T> GroupBind<R> subGroup(GroupBind parent, String subGroupName, Invocable<? extends R> constructor) {
        final GroupBind<R> groupBind = new GroupBind<>(this, serializationAdapter, subGroupName, constructor);
        parent.subgroups.add(groupBind);
        return groupBind;
    }

    public VarBindBuilder<T, ?, ?, ?> createBind(String fieldName) {
        return new BindingBuilder<>(this, fieldName);
    }

    @Internal
    public void addChild(VarBind<T, ?, ?, ?> child) {
        children.add(Polyfill.uncheckedCast(child));
    }

    @NotNull
    @Override
    public Iterator<GroupBind<? extends T>> iterator() {
        return subgroups.iterator();
    }
}
