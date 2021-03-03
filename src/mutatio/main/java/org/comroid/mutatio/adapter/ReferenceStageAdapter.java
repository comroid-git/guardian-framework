package org.comroid.mutatio.adapter;

import org.comroid.mutatio.pipe.ReferenceConverter;
import org.comroid.mutatio.ref.Reference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ReferenceStageAdapter<InK, OutK, InV, OutV, InRef extends Reference<InV>, OutRef extends Reference<OutV>>
        implements ReferenceConverter<InRef, OutRef> {
    private final boolean isIdentity;
    private final Function<@NotNull ? super InK, @NotNull ? extends OutK> keyMapper;
    private final BiFunction<? super InK, ? super InV, @Nullable ? extends OutV> valueMapper;
    private final @Nullable Function<? super OutK, ? extends InK> keyReverser;
    private final @Nullable Function<? super OutV, ? extends InV> valueReverser;

    public final boolean isIdentityValue() {
        return isIdentity;
    }

    protected ReferenceStageAdapter(
            boolean isIdentity,
            Function<@NotNull ? super InK, @NotNull ? extends OutK> keyMapper,
            BiFunction<? super InK, ? super InV, @Nullable ? extends OutV> valueMapper
    ) {
        this(isIdentity, keyMapper, valueMapper, null, null);
    }

    protected ReferenceStageAdapter(
            boolean isIdentity,
            Function<@NotNull ? super InK, @NotNull ? extends OutK> keyMapper,
            BiFunction<? super InK, ? super InV, @Nullable ? extends OutV> valueMapper,
            @Nullable Function<? super OutK, ? extends InK> keyReverser,
            @Nullable Function<? super OutV, ? extends InV> valueReverser
    ) {
        this.isIdentity = isIdentity;
        this.keyMapper = keyMapper;
        this.valueMapper = valueMapper;
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

    public final Optional<InK> revertKey(OutK key) {
        if (keyReverser == null)
            return Optional.empty();
        return Optional.ofNullable(keyReverser.apply(key));
    }

    public final Optional<InV> revertValue(OutV value) {
        if (valueReverser == null)
            return Optional.empty();
        return Optional.ofNullable(valueReverser.apply(value));
    }

}
