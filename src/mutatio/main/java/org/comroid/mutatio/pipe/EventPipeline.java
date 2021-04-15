package org.comroid.mutatio.pipe;

import org.comroid.mutatio.model.RefContainer;

import java.util.Objects;
import java.util.function.BiPredicate;

public interface EventPipeline<ID, T> {
    RefContainer<ID, T> getEventPipeline();

    default RefContainer<ID, T> on(final ID identifier) {
        final BiPredicate<ID, ID> comparison = idComparison();
        return getEventPipeline().filterKey(id -> comparison.test(id, identifier));
    }

    default BiPredicate<ID, ID> idComparison() {
        return Objects::equals;
    }
}
