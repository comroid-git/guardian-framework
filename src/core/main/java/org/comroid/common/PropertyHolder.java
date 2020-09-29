package org.comroid.common;

import org.comroid.api.UUIDContainer;
import org.comroid.mutatio.ref.KeyedReference;
import org.comroid.mutatio.ref.ReferenceIndex;
import org.comroid.mutatio.ref.ReferenceMap;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public interface PropertyHolder extends UUIDContainer {
    default ReferenceMap<String, Object> getPropertyCache() {
        return Support.getCache(this);
    }

    @Internal
    final class Support {
        private static final Map<UUID, ReferenceMap<String, Object>> cache = new ConcurrentHashMap<>();

        private static ReferenceMap<String, Object> getCache(PropertyHolder holder) {
            return cache.computeIfAbsent(holder.getUUID(), k -> ReferenceMap.create());
        }
    }
}
