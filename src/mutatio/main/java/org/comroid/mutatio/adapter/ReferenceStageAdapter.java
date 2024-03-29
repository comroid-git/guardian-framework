package org.comroid.mutatio.adapter;

import org.comroid.mutatio.model.RefAtlas;
import org.comroid.mutatio.pipe.ReferenceConverter;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ReferenceStageAdapter<InK, OutK, InV, OutV, InRef extends Reference<InV>, OutRef extends Reference<OutV>>
        implements ReferenceConverter<InRef, OutRef> {
    protected final Function<@NotNull ? super InK, @NotNull ? extends OutK> keyMapper;
    protected final BiFunction<? super InK, ? super InV, @Nullable ? extends OutV> valueMapper;
    protected final @Nullable Function<? super OutK, ? extends InK> keyReverser;
    protected final @Nullable Function<? super OutV, ? extends InV> valueReverser;
    private final boolean isFiltering;

    public final boolean isFiltering() {
        return isFiltering;
    }

    @Deprecated
    protected ReferenceStageAdapter(
            boolean isFiltering,
            @NotNull Function<@NotNull ? super InK, @NotNull ? extends OutK> keyMapper,
            @NotNull BiFunction<? super InK, ? super InV, @Nullable ? extends OutV> valueMapper
    ) {
        this(isFiltering, keyMapper, valueMapper, null, null);
    }

    protected ReferenceStageAdapter(
            boolean isFiltering,
            @NotNull Function<@NotNull ? super InK, @NotNull ? extends OutK> keyMapper,
            @NotNull BiFunction<? super InK, ? super InV, @Nullable ? extends OutV> valueMapper,
            @Nullable Function<? super OutK, ? extends InK> keyReverser,
            @Nullable Function<? super OutV, ? extends InV> valueReverser
    ) {
        this.isFiltering = isFiltering;
        this.keyMapper = Objects.requireNonNull(keyMapper, "key mapper");
        this.valueMapper = Objects.requireNonNull(valueMapper, "value mapper");
        this.keyReverser = keyReverser;
        this.valueReverser = valueReverser;
    }

    @Override
    public abstract OutRef advance(InRef reference);

    public final @NotNull OutK advanceKey(@NotNull InK key) {
        return keyMapper.apply(key);
    }

    public final @Nullable OutV advanceValue(InK key, InV value) {
        return valueMapper.apply(key, value);
    }

    public Optional<InK> revertKey(OutK key) {
        if (keyReverser == null)
            return Optional.empty();
        return Optional.ofNullable(keyReverser.apply(key));
    }

    public Optional<InV> revertValue(OutV value) {
        if (valueReverser == null)
            return Optional.empty();
        return Optional.ofNullable(valueReverser.apply(value));
    }

    @Internal
    @Contract("null, _ -> null; !null, _ -> _")
    public <DK, DV> InK findParentKey(@Nullable RefAtlas<DK, InK, DV, InV> parent, @NotNull OutK targetKey) {
        if (parent == null)
            return null;
        return revertKey(targetKey)
                .orElseGet(() -> parent.streamKeys()
                        .filter(inK -> advanceKey(inK).equals(targetKey))
                        .findFirst()
                        .orElse(null));
    }
}
