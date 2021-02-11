package org.comroid.mutatio.pipe.impl;

import org.comroid.mutatio.cache.ValueCache;
import org.comroid.mutatio.pipe.Pipe;

@Deprecated
public class BasicPipe<O, T> extends ValueCache.Underlying<Void> implements Pipe<T> {
    protected BasicPipe(ValueCache<Void> underlying) {
        super(underlying);
    }
}
