package org.comroid.mutatio.model;

import org.comroid.api.ValueProvider;

public interface ParamRef<I, O> extends UncachedRef<O>, ValueProvider<I, O> {
}
