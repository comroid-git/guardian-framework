package org.comroid.mutatio.model;

import org.comroid.api.Rewrapper;
import org.comroid.api.ValueBox;
import org.comroid.api.ValueType;
import org.comroid.mutatio.cache.SingleValueCache;
import org.comroid.mutatio.ref.ParameterizedReference;
import org.comroid.mutatio.ref.Reference;
import org.comroid.util.StandardValueType;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.*;

public interface Ref<T> extends UncachedRef<T>, SingleValueCache<T> {
}
