package org.comroid.varbind;

import org.comroid.api.Invocable;
import org.comroid.api.Polyfill;
import org.comroid.common.info.Dependent;
import org.comroid.mutatio.proc.Processor;
import org.comroid.uniform.cache.BasicCache;
import org.comroid.uniform.cache.Cache;
import org.comroid.uniform.cache.CacheReference;
import org.comroid.uniform.node.UniObjectNode;
import org.comroid.varbind.bind.GroupBind;
import org.comroid.varbind.bind.VarBind;
import org.comroid.varbind.container.DataContainer;
import org.comroid.varbind.container.DataContainerBase;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

public abstract class DataContainerCache<K, V extends DataContainer<? super V>, D>
        extends BasicCache<K, V>
        implements Cache<K, V>, Dependent<D> {
    protected final VarBind<? super V, ?, ?, K> idBind;
    private final @Nullable D dependencyObject;

    @Override
    public final @Nullable D getDependent() {
        return dependencyObject;
    }

    public DataContainerCache(
            int largeThreshold,
            Map<K, CacheReference<K, V>> map,
            VarBind<? super V, ?, ?, K> idBind
    ) {
        this(largeThreshold, map, idBind, null);
    }

    public DataContainerCache(
            int largeThreshold,
            Map<K, CacheReference<K, V>> map,
            VarBind<? super V, ?, ?, K> idBind,
            @Nullable D dependencyObject
    ) {
        super(largeThreshold, map);

        this.idBind = Polyfill.uncheckedCast(idBind);
        this.dependencyObject = dependencyObject;
    }

    public boolean add(V value) {
        final K key = value.requireNonNull(Polyfill.uncheckedCast(idBind));

        return set(key, value);
    }

    public boolean remove(V value) {
        final K key = value.requireNonNull(Polyfill.uncheckedCast(idBind));

        return containsKey(key) && set(key, null);
    }

    public final Processor<V> autoUpdate(UniObjectNode data) {
        return autoUpdate(Polyfill.<GroupBind<V>>uncheckedCast(idBind.getGroup()), data);
    }

    public final <T extends V> Processor<T> autoUpdate(Class<T> type, UniObjectNode data) {
        return autoUpdate(DataContainerBase.findRootBind(type), data);
    }

    public final <T extends V> Processor<T> autoUpdate(GroupBind<? extends T> group, UniObjectNode data) {
        return autoUpdate(group.getConstructor()
                .orElseThrow(() -> new NoSuchElementException("No constructor defined in group " + group)), data);
    }

    public final <T extends V> Processor<T> autoUpdate(Invocable<? extends T> creator, UniObjectNode data) {
        final K key = idBind.getFrom(data);

        if (containsKey(key))
            //noinspection unchecked
            return getReference(key, false)
                    .process()
                    .peek(it -> it.updateFrom(data))
                    .map(it -> (T) it);
        else //noinspection unchecked
            return Processor.ofConstant(tryConstruct(data))
                    .map(opt -> {
                        if (!opt.isPresent())
                            return creator.autoInvoke(data, dependencyObject);
                        return opt.get();
                    })
                    .map(it -> (T) it)
                    .peek(it -> getReference(key, true).set(it));
    }

    protected Optional<? extends V> tryConstruct(UniObjectNode node) {
        //noinspection unchecked
        return (Optional<? extends V>) idBind.getGroup().findGroupForData(node)
                .flatMap(GroupBind::getConstructor)
                .map(constr -> constr.autoInvoke(dependencyObject, node));
    }
}
