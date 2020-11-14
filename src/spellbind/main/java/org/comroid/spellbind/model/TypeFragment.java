package org.comroid.spellbind.model;

import org.comroid.api.Rewrapper;
import org.comroid.api.Specifiable;
import org.comroid.common.exception.AssertionException;
import org.comroid.spellbind.SpellCore;

import java.util.Optional;
import java.util.UUID;

public interface TypeFragment<S extends TypeFragment<? super S>> extends Specifiable<S> {
    UUID getUUID();

    @Override
    default <R extends S> Optional<R> as(Class<R> type) {
        return SpellCore.<S>getCore(this)
                .map(SpellCore::self)
                .map(Rewrapper::get)
                .filter(type::isInstance)
                .map(type::cast);
    }

    @Override
    default Rewrapper<S> self() {
        return SpellCore.<S>getCore(this)
                .map(SpellCore::self)
                .orElseThrow(AssertionException::new);
    }
}
