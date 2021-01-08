package org.comroid.varbind.container;

import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.api.SelfDeclared;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.Processor;
import org.comroid.mutatio.ref.Reference;
import org.comroid.mutatio.ref.ReferenceMap;
import org.comroid.mutatio.span.Span;
import org.comroid.uniform.ValueType;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.ReflectionHelper;
import org.comroid.varbind.annotation.Location;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.multipart.PartialBind;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.annotation.ElementType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableSet;
import static org.comroid.api.Polyfill.uncheckedCast;

@SuppressWarnings("unchecked")
public class DataContainerBase<S extends DataContainer<? super S> & SelfDeclared<? super S>>
        extends AbstractMap<String, Object>
        implements DataContainer<S> {
    private final GroupBind<S> rootBind;
    private final Map<String, VarBind<? extends S, Object, Object, Object>> binds;
    private final ReferenceMap<String, Span<Object>> baseRefs;
    private final ReferenceMap<String, Object> computedRefs;
    private final Set<VarBind<? extends S, Object, ?, Object>> initiallySet;
    private final Class<? extends S> myType;
    private final Supplier<S> selfSupplier;

    @Override
    public final GroupBind<S> getRootBind() {
        return rootBind;
    }

    @Override
    public Class<? extends S> getRepresentedType() {
        return myType;
    }

    public DataContainerBase(
            @Nullable UniObjectNode initialData
    ) {
        this(initialData, null, null);
    }

    @Contract("_, null, !null -> fail; _, !null, null -> fail")
    public DataContainerBase(
            @Nullable UniObjectNode initialData,
            Class<? extends DataContainer<? super S>> containingClass,
            Supplier<S> selfSupplier
    ) {
        if ((containingClass == null) != (selfSupplier == null))
            throw new IllegalArgumentException("Not both containingClass and selfSupplier have been provided!");

        this.myType = (Class<? extends S>) (containingClass == null ? getClass() : containingClass);
        this.selfSupplier = selfSupplier;
        this.baseRefs = ReferenceMap.create();
        this.rootBind = findRootBind(myType);
        this.binds = findAllBinds(rootBind);
        this.computedRefs = baseRefs
                .biPipe()
                .mapKey(key -> ((VarBind<? extends S, Object, Object, Object>) binds.get(key)))
                .mapBoth(PartialBind.Finisher::finish)
                .mapKey(PartialBind.Base::getName);
        this.initiallySet = unmodifiableSet(updateVars(initialData));
    }

    @Contract("_, null, !null -> fail; _, !null, null -> fail")
    public DataContainerBase(
            @NotNull Map<VarBind<? extends S, Object, ?, Object>, Object> initialValues,
            Class<? extends DataContainer<? super S>> containingClass,
            Supplier<S> selfSupplier
    ) {
        if ((containingClass == null) != (selfSupplier == null))
            throw new IllegalArgumentException("Not both containingClass and selfSupplier have been provided!");

        this.myType = (Class<? extends S>) (containingClass == null ? getClass() : containingClass);
        this.selfSupplier = selfSupplier;
        this.baseRefs = ReferenceMap.create();
        this.rootBind = findRootBind(myType);
        this.binds = findAllBinds(rootBind);
        this.initiallySet = unmodifiableSet(initialValues.keySet());
        initialValues.forEach((bind, value) -> getExtractionReference(bind).set(Span.singleton(value)));
        this.computedRefs = baseRefs
                .biPipe()
                .mapKey(key -> ((VarBind<? extends S, Object, Object, Object>) binds.get(key)))
                .mapBoth(PartialBind.Finisher::finish)
                .mapKey(PartialBind.Base::getName);
    }

    @Internal
    public static <T extends DataContainer<? super T>> GroupBind<T> findRootBind(Class<? extends T> inClass) {
        final Location location = ReflectionHelper.findAnnotation(Location.class, inClass, ElementType.TYPE).orElse(null);

        final Iterator<GroupBind<T>> groups = ReflectionHelper
                .<GroupBind<T>>collectStaticFields(
                        uncheckedCast(GroupBind.class),
                        location == null ? inClass : location.value(),
                        true,
                        RootBind.class
                ).iterator();
        if (!groups.hasNext())
            throw new NoSuchElementException(String.format("No @RootBind annotated field found in %s", location.value()));
        return groups.next();
    }

    private static <S extends DataContainer<? super S>>
    Map<String, VarBind<? extends S, Object, Object, Object>>
    findAllBinds(GroupBind<S> rootBind) {
        final Map<String, VarBind<? extends S, Object, Object, Object>> map = new ConcurrentHashMap<>();
        rootBind.streamAllChildren()
                .forEach(bind -> map.put(
                        bind.getFieldName(),
                        (VarBind<? extends S, Object, Object, Object>) bind)
                );
        return Collections.unmodifiableMap(map);
    }

    @Override
    public Rewrapper<S> self() {
        return selfSupplier == null ? () -> Polyfill.uncheckedCast(this) : Rewrapper.ofSupplier(selfSupplier);
    }

    private Set<VarBind<? extends S, Object, ?, Object>> updateVars(
            @Nullable UniObjectNode data
    ) {
        if (data == null) {
            return emptySet();
        }

        final GroupBind<S> rootBind = getRootBind();
        if (!rootBind.isValidData(data))
            throw new IllegalArgumentException(String.format("Data is invalid for type '%s': %s", rootBind, data));

        final HashSet<VarBind<? extends S, Object, ?, Object>> changed = new HashSet<>();

        rootBind.streamAllChildren()
                .map(it -> (VarBind<? extends S, Object, ?, Object>) it)
                .filter(bind -> data.has(bind.getFieldName()))
                .map(it -> (VarBind<? extends S, Object, Object, Object>) it)
                .forEach(bind -> {
                    Span<Object> extract = bind.extract(data);

                    getExtractionReference(bind).set(extract);
                    getComputedReference(bind).outdate();
                    changed.add(bind);
                });

        return unmodifiableSet(changed);
    }

    public boolean containsKey(VarBind<? extends S, Object, Object, Object> bind) {
        return computedRefs.containsKey(bind.getFieldName());
    }

    @Override
    public final Set<VarBind<? extends S, Object, ?, Object>> updateFrom(UniObjectNode node) {
        return unmodifiableSet(updateVars(node));
    }

    @Override
    public final Set<VarBind<? extends S, Object, ?, Object>> initiallySet() {
        return initiallySet;
    }

    @Override
    public final <T> Optional<Reference<T>> getByName(String name) {
        final String[] split = name.split("\\.");

        if (split.length == 1) {
            return getRootBind().streamAllChildren()
                    .filter(bind -> bind.getFieldName().equals(name))
                    .findAny()
                    .map(it -> getComputedReference(it.getFieldName()));
        }

        // any stage in the groupbind tree
        Processor<GroupBind<? super S>> parentGroup = Processor.ofConstant(getRootBind());

        // find the topmost parent
        while (parentGroup.requireNonNull()
                .getParents().isSingle()) {
            parentGroup = parentGroup.map(group -> group.getParents()
                    .wrap()
                    .orElse(uncheckedCast(group)));
        }

        // find the subgroup named the first split part,
        return parentGroup.into(parent -> parent.getSubgroups().stream())
                .filter(group -> group.getName().equals(split[0]))
                // then find the subgroup named second split part
                .flatMap(GroupBind::streamAllChildren)
                .filter(bind -> bind.getFieldName().equals(split[1]))
                .findAny()
                // get reference of bind
                .map(it -> getComputedReference(it.getFieldName()));
    }

    @Override
    public UniObjectNode toObjectNode(UniObjectNode applyTo) {
        binds.keySet().forEach(key -> {
            final @NotNull Span<Object> them = getExtractionReference(key).requireNonNull("Span is null");

            if (them.isEmpty()) {
                final Reference<Object> ref = computedRefs.getReference(key);
                final Object it = ref.get();

                // support array binds
                if (it instanceof Collection) {
                    final UniArrayNode array = applyTo.putArray(key);
                    //noinspection rawtypes
                    ((Collection) it).forEach(each -> applyValueToNode(array.addObject(), key, each));

                    return;
                }

                if (it != null)
                    applyValueToNode(applyTo, key, it);
                return;
            }

            if (them.isSingle()) {

                final Reference<?> comp = getComputedReference(binds.get(key));

                if (comp.test(DataContainer.class::isInstance)) {
                    applyValueToNode(applyTo, key, comp.flatMap(DataContainer.class)
                            .into(db -> db.toObjectNode(rootBind.getFromContext())));
                } else applyValueToNode(applyTo, key, them.requireNonNull("AssertionFailure"));
            } else {
                final UniArrayNode array = applyTo.putArray(key);
                them.forEach(it -> applyValueToNode(array.addObject(), key, it));
            }
        });

        return applyTo;
    }

    private UniNode applyValueToNode(UniObjectNode applyTo, String key, Object it) {
        if (it instanceof DataContainer)
            return ((DataContainer<? super S>) it).toObjectNode(applyTo.putObject(key));
        else if (it instanceof UniNode)
            return applyTo.putObject(key).copyFrom((UniNode) it);
        else return applyTo.put(key, ValueType.STRING, String.valueOf(it));
    }

    @Override
    public <T> @Nullable T put(VarBind<? extends S, T, ?, ?> bind, final T value) {
        final Reference<Span<T>> extRef = getExtractionReference(bind.getFieldName());
        T prev = extRef.into(Span::get);

        extRef.compute(span -> {
            if (span == null || span.isFixedSize())
                return Span.<T>make()
                        .initialValues(value)
                        .fixedSize(!bind.isListing())
                        .span();
            span.add(value);
            return span;
        });

        getComputedReference(bind).outdate();

        return prev;
    }

    @Override
    public <R, T> @Nullable R put(VarBind<? extends S, T, ?, R> bind, Function<R, T> parser, R value) {
        final T apply = parser.apply(value);
        final R prev = getComputedReference(bind).get();

        if (bind.isListing()) {
            getExtractionReference(bind).compute(span -> {
                span.add(apply);
                return span;
            });

            if (prev != null)
                ((Collection<R>) prev).addAll((Collection<R>) value);
            else getComputedReference(bind).update((R) Span.singleton(value));
        } else {
            getExtractionReference(bind).set(Span.singleton(apply));
            getComputedReference(bind.getFieldName()).update(value);
        }

        return prev;
    }

    @Override
    public <E> KeyedReference<String, Span<E>> getExtractionReference(String fieldName) {
        baseRefs.computeIfAbsent(fieldName, Span::new);
        return uncheckedCast(Objects.requireNonNull(
                baseRefs.getReference(fieldName),
                String.format("Missing base reference %s @ %s", fieldName, toString())));
    }

    @Override
    public <T, E> KeyedReference<String, T> getComputedReference(String fieldName) {
        return uncheckedCast(Objects.requireNonNull(
                computedRefs.getReference(fieldName, true),
                String.format("Missing computed reference %s @ %s", fieldName, toString())));
    }

    @Override
    public String toString() {
        return String.format("DataContainerBase{rootBind=%s, binds=%s}", rootBind, binds);
    }

    @NotNull
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return unmodifiableSet(computedRefs.entryIndex()
                .stream()
                .collect(Collectors.toSet()));
    }

    public class ComputedReference<T, E> extends Reference.Support.Base<T> {
        private final VarBind<S, E, ?, T> bind;
        private final Processor<T> accessor;

        public ComputedReference(VarBind<? extends S, E, ?, T> bind) {
            super(false); // todo Implement reverse binding

            this.bind = uncheckedCast(bind);
            this.accessor = getExtractionReference(bind)
                    .map(extr -> this.bind.process(self().get(), extr));
        }

        @Override
        public final @Nullable T doGet() {
            if (!isOutdated())
                return super.get();
            return update(accessor.get());
        }
    }
}
