package de.foxat.mercury.base.modules;

import java.util.concurrent.atomic.AtomicInteger;

public class ModuleDisableTask extends Thread {

    private static final AtomicInteger counter = new AtomicInteger();

    private final ModuleLoader loader;

    protected ModuleDisableTask(ModuleLoader loader) {
        this.loader = loader;
        this.setName("ModuleShutdownHook-" + counter.getAndIncrement());
    }

    public static void addShutdownHook(ModuleLoader loader) {
        Runtime.getRuntime().addShutdownHook(new ModuleDisableTask(loader));
    }

    @Override
    public void run() {
        loader.disableLoadedModules();
    }
}
