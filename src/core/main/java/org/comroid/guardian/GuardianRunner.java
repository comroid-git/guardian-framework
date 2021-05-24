package org.comroid.guardian;

import org.comroid.mutatio.model.Ref;
import org.comroid.mutatio.ref.FutureReference;

public final class GuardianRunner implements Runnable {
    public static final ThreadGroup GROUP = new ThreadGroup("guardian");
    private static final Ref<GuardianRunner> instance = new FutureReference<>();
    private final Class<? extends GuardianModule>[] modules;

    @SafeVarargs
    public GuardianRunner(Class<? extends GuardianModule>... modules) {
        this.modules = modules;
    }

    public static void main(String[] args) {
        GuardianRunner thread = new GuardianRunner();
    }

    @Override
    public void run() {

    }
}
