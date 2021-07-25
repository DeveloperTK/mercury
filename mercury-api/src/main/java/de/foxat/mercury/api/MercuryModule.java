package de.foxat.mercury.api;

import de.foxat.mercury.api.config.AbstractModuleConfig;

public abstract class MercuryModule {

    private boolean enabledByDefault;

    public MercuryModule() {
        enabledByDefault = true;
    }

    protected abstract void onLoad();

    protected abstract void onEnable();

    protected abstract void onDisable();

    public final void doLoad() {
        onLoad();
    }

    public final void tryEnable() {
        onEnable();
    }

    public final void doDisable() {
        onDisable();
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public void setConfig(AbstractModuleConfig config) {

    }
}
