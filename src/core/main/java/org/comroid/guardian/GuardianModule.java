package org.comroid.guardian;

import org.comroid.api.ContextualProvider;
import org.comroid.api.LifeCycle;
import org.comroid.api.UUIDContainer;
import org.comroid.common.io.FileProcessor;

import java.util.UUID;

public abstract class GuardianModule extends UUIDContainer.Base implements LifeCycle, FileProcessor, ContextualProvider.Underlying {
    private final ContextualProvider context;

    @Override
    public final ContextualProvider getUnderlyingContextualProvider() {
        return context;
    }

    public GuardianModule(ContextualProvider context) {
        this(context, null);
    }

    public GuardianModule(ContextualProvider context, UUID uuid) {
        super(uuid);

        this.context = context;
    }

    @Override
    public final void close() throws MultipleExceptions {
        FileProcessor.super.close();
    }

    @Override
    public void closeSelf() throws Throwable {
        FileProcessor.super.closeSelf();
        deinitialize();
    }
}
