package org.comroid.varbind.container;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.comroid.api.ContextualProvider;
import org.comroid.api.Polyfill;
import org.comroid.api.Rewrapper;
import org.comroid.api.ThrowingFunction;
import org.comroid.common.info.MessageSupplier;
import org.comroid.mutatio.adapter.BiStageAdapter;
import org.comroid.mutatio.adapter.ReferenceStageAdapter;
import org.comroid.mutatio.ref.*;
import org.comroid.mutatio.stack.RefStack;
import org.comroid.mutatio.stack.RefStackUtil;
import org.comroid.uniform.SerializationAdapter;
import org.comroid.uniform.node.UniArrayNode;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.util.ReflectionHelper;
import org.comroid.varbind.annotation.RootBind;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DataContainerBase<S extends DataContainer<? super S>>
        extends ReferenceAtlas.ForMap<String, ReferenceList, VarBind, Object>
        implements DataContainer<S> {
    private static final Logger logger = LogManager.getLogger();
    private final ReferenceStageAdapter<String, VarBind, ReferenceList, Object, KeyedReference<String, ReferenceList>, KeyedReference<VarBind, Object>> adapter;
    private final ContextualProvider context;
    private final GroupBind<S> group;
    private final Set<VarBind<? extends S, Object, ?, Object>> initialValues;
    private final Function<UniObjectNode, UniObjectNode> parser;

    @Override
    public final GroupBind<S> getRootBind() {
        return group;
    }

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    @Override
    public final ReferenceStageAdapter<String, VarBind, ReferenceList, Object, KeyedReference<String, ReferenceList>, KeyedReference<VarBind, Object>> getAdvancer() {
        return adapter;
    }

    protected DataContainerBase(
            ContextualProvider context
    ) {
        this(context, (UniObjectNode) null);
    }

    protected DataContainerBase(
            ContextualProvider context,
            Consumer<UniObjectNode> initialDataBuilder
    ) {
        this(context, initialDataBuilder == null ? null : context.requireFromContext(SerializationAdapter.class).createObjectNode(initialDataBuilder));
    }

    protected DataContainerBase(
            ContextualProvider context,
            @Nullable UniObjectNode initialData
    ) {
        this(null, context, initialData);
    }

    protected DataContainerBase(
            @Nullable GroupBind<S> group,
            ContextualProvider context,
            UniObjectNode initialData
    ) {
        //noinspection ConstantConditions allowed here because we overwrite getAdvancer()
        super(new ReferenceMap<>(true), null);
        if (isImmutable())
            setMutable();
        this.context = context;
        //noinspection unchecked
        this.group = group != null ? group : findRootBind(getClass());
        this.adapter = new BindBasedAdapter();
        this.initialValues = updateFrom(initialData);
        this.parser = new ParameterizedReference<UniObjectNode, UniObjectNode>(this, null) {
            @Override
            protected UniObjectNode doGet(final UniObjectNode into) {
                streamInputRefs().forEach(ref -> ref
                        .consume(refs -> {
                            //noinspection StatementWithEmptyBody
                            if (refs == null || refs.size() == 0) {
                            } else if (refs.size() > 1) {
                                UniArrayNode arr = into.putArray(ref.getKey());
                                //noinspection unchecked
                                refs.forEach(arr::addValue);
                            } else {
                                Object v = refs.get(0);
                                if (v instanceof Map) {
                                    UniObjectNode obj = into.putObject(ref.getKey());
                                    //noinspection unchecked
                                    ((Map<String, Object>) v).forEach(obj::put);
                                } else into.put(ref.getKey(), v);
                            }
                        }));
                return into;
            }
        };
    }

    public static <S extends DataContainer<? super S>> GroupBind<S> findRootBind(Class<? extends S> aClass) {
        return ReflectionHelper.fieldWithAnnotation(aClass, RootBind.class)
                .stream()
                .filter(field -> Modifier.isStatic(field.getModifiers()))
                .findAny()
                .map(ThrowingFunction.rethrowing(field -> field.get(null), RuntimeException::new))
                .map(Polyfill::<GroupBind<S>>uncheckedCast)
                .orElseThrow(() -> new NoSuchElementException("No RootBind found in class " + aClass));
    }

    @Override
    public final Set<VarBind<? extends S, Object, ?, Object>> updateFrom(UniObjectNode node) {
        if (node == null)
            return Collections.emptySet();
        final Set<VarBind<? extends S, Object, ?, Object>> initialized = new HashSet<>();
        node.forEach((key, value) -> {
            KeyedReference<String, ReferenceList<Object>> eRef = getExtractionReference(key);
            VarBind<? extends S, ?, ?, Object> bind = getBindByName(key);

            if (bind == null) {
                logger.warn("No bind found for key {}; skipping", key);
                return;
            }

            eRef.compute(refs -> {
                if (refs == null)
                    refs = new ReferenceList<>();
                else refs.clear();
                refs.add(value);
                return refs;
            });
            KeyedReference<VarBind, Object> cRef = getComputedReference(bind);
            Object prev;
            try {
                prev = cRef.get();
            } catch (ClassCastException cce) {
                throw new IllegalStateException(getRootBind().getName() + ": Data has invalid structure; failed at key " + key, cce);
            }
            if (cRef.get() != prev)
                initialized.add(Polyfill.uncheckedCast(bind));
        });
        return Collections.unmodifiableSet(initialized);
    }

    @Override
    public final Set<VarBind<? extends S, Object, ?, Object>> initiallySet() {
        return initialValues;
    }

    @Override
    public final UniObjectNode toObjectNode(final UniObjectNode node) {
        return parser.apply(node);
    }

    @Override
    public final <E> KeyedReference<String, ReferenceList<Object>> getExtractionReference(String name) {
        return Polyfill.uncheckedCast(getInputReference(name, true));
    }

    @Override
    public final <T> KeyedReference<VarBind, T> getComputedReference(VarBind<?, ?, ?, T> bind) {
        return Polyfill.uncheckedCast(getReference(bind, true));
    }

    @Override
    public final VarBind<? extends S, ?, ?, Object> getBindByName(String name) {
        //noinspection unchecked
        return getAdvancer().advanceKey(name);
    }

    @Override
    protected final KeyedReference<VarBind, Object> createEmptyRef(VarBind bind) {
        //noinspection unchecked
        KeyedReference<String, ReferenceList> base = getExtractionReference(bind);
        if (base == null)
            throw new NullPointerException("Could not create base reference");
        return advanceReference(base);
    }

    @Override
    protected final KeyedReference<VarBind, Object> advanceReference(KeyedReference<String, ReferenceList> ref) {
        RefStack<? extends VarBind<? extends S, ?, ?, Object>> keyStack = RefStackUtil.$map(ref.keyStack(), this::getBindByName);
        RefStack<Object> valueStack = RefStackUtil.$combine(ref.valueStack(), keyStack, (refs, bind) -> {
            if (bind == null)
                throw new NoSuchElementException("Bind not found!");
            return bind.process(self().cast(), refs);
        });
        return new KeyedReference<>(Polyfill.uncheckedCast(keyStack), Polyfill.uncheckedCast(valueStack));
    }

    private <T, R> R computeValueFor(VarBind<? super S, T, ?, R> bind, ReferenceList<T> fromBase) {
        for (VarBind<?, ?, ?, ?> dependency : bind.getDependencies())
            getExtractionReference(dependency).requireNonNull(MessageSupplier
                    .format("Could not compute dependency %s of bind %s", dependency, bind));
        return bind.process(self().into(Polyfill::uncheckedCast), fromBase);
    }

    @Override
    public final Rewrapper<? extends ContextualProvider> self() {
        return () -> this;
    }

    @Nullable
    @Override
    public final Object put(VarBind key, Object value) {
        return put(key.getFieldName(), value);
    }

    @NotNull
    @Override
    public final Set<Entry<VarBind, Object>> entrySet() {
        return Collections.unmodifiableSet(streamRefs().collect(Collectors.toSet()));
    }

    private class BindBasedAdapter extends BiStageAdapter<String, ReferenceList, VarBind, Object> {
        public BindBasedAdapter() {
            super(false, group::findChildByName, (fname, referenceList) -> {
                VarBind<? super S, ?, ?, ?> bind = group.findChildByName(fname);
                if (bind == null)
                    return null;
                //noinspection unchecked
                return bind.process(Polyfill.uncheckedCast(DataContainerBase.this), referenceList);
            }, VarBind::getFieldName, null);
        }

        @Override
        public KeyedReference<VarBind, Object> advance(KeyedReference<String, ReferenceList> ref) {
            RefStack<VarBind> keyStack = RefStackUtil.$map(ref.keyStack(), this::advanceKey);
            RefStack<Object> valueStack = RefStackUtil.$combine(ref.valueStack(), ref.keyStack(), (refs, bind) -> advanceValue(bind, refs));
            return new KeyedReference<>(keyStack, valueStack);
        }
    }
}
