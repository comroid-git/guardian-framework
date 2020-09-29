package org.comroid.common.ref;

import org.comroid.mutatio.ref.Reference;
import org.comroid.util.StackTraceUtils;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collection;

public abstract class CloseableReference<T> implements Reference<T>, Closeable {
    private final Collection<ClosedEvent.Listener<T>> listeners = new ArrayList<>();

    @Override
    public final void close() {
        final ClosedEvent<T> closedEvent = new ClosedEvent<>(this, StackTraceUtils.callerClass(1));
        listeners.forEach(each -> each.onReferenceClosed(closedEvent));
        unsetReferences();
        try {
            //noinspection FinalizeCalledExplicitly,deprecation
            finalize();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    protected abstract void unsetReferences();

    public final void addCloseListener(ClosedEvent.Listener<T> listener) {
        listeners.add(listener);
    }

    public static class ClosedEvent<T> {
        private final CloseableReference<T> ref;
        private final Class<?> closing;

        public CloseableReference<T> getReference() {
            return ref;
        }

        public Class<?> getClosingClass() {
            return closing;
        }

        private ClosedEvent(CloseableReference<T> ref, Class<?> closing) {
            this.ref = ref;
            this.closing = closing;
        }

        @FunctionalInterface
        interface Listener<T> {
            void onReferenceClosed(ClosedEvent<T> closedEvent);
        }
    }
}
