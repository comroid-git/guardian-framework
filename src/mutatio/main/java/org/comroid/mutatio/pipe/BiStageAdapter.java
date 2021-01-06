package org.comroid.mutatio.pipe;

import org.comroid.mutatio.ref.KeyedReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface BiStageAdapter<InX, InY, OutX, OutY> extends StageAdapter<InY, OutY, KeyedReference<InX, InY>, KeyedReference<OutX, OutY>> {
    @Override
    KeyedReference<OutX, OutY> advance(KeyedReference<InX, InY> ref);

    final class Support {
        private final static class Filter<X, Y> implements BiStageAdapter<X, Y, X, Y> {
            private final Predicate<@NotNull X> keyFilter;
            private final Predicate<@Nullable Y> valueFilter;

            private Filter(Predicate<@NotNull X> keyFilter, Predicate<@Nullable Y> valueFilter) {
                this.keyFilter = keyFilter;
                this.valueFilter = valueFilter;
            }

            @Override
            public KeyedReference<X, Y> advance(KeyedReference<X, Y> ref) {
                if (keyFilter.test(ref.getKey()) && ref.test(valueFilter))
                    return ref;
                return null;
            }
        }

        private final static class Map<IX, IY, OX, OY> implements BiStageAdapter<IX, IY, OX, OY> {
            @Override
            public KeyedReference<OX, OY> advance(KeyedReference<IX, IY> ref) {
                return null; // todo
            }
        }
    }
}
