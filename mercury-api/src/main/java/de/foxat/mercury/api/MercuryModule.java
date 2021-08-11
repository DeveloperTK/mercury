package de.foxat.mercury.api;

import de.foxat.mercury.api.config.AbstractModuleProperties;
import de.foxat.mercury.api.config.ModulePropertyField;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class MercuryModule {

    private boolean enabledByDefault;

    private AbstractModuleProperties config;
    private Mercury mercury;

    private final Logger logger;

    public MercuryModule() {
        logger = LogManager.getLogger();
        enabledByDefault = true;
    }

    protected abstract void onLoad();

    protected abstract void onEnable();

    protected abstract void onDisable();

    protected void onCommand(SlashCommandEvent slashCommandEvent) {
        logger.warn("No implementation for slash command event in module {}. Dispatched command was: {}",
                getConfig().getProperty(ModulePropertyField.NAME),
                slashCommandEvent.getName()
        );
    }

    public final void doLoad() {
        onLoad();
    }

    public final void tryEnable() {
        onEnable();
    }

    public final void doDisable() {
        onDisable();
    }

    public final void dispatchCommandEvent(SlashCommandEvent slashCommandEvent) {
        try {
            onCommand(slashCommandEvent);
        } catch (Exception exception) {
            logger.error(String.format("Caught exception in command handler for %s in module %s",
                    slashCommandEvent.getName(),
                    getConfig().getProperty(ModulePropertyField.NAME)
            ), exception);
        }
    }

    public Logger getLogger() {
        return logger;
    }

    public Mercury getMercury() {
        return mercury;
    }

    public void setEnabledByDefault(boolean enabledByDefault) {
        this.enabledByDefault = enabledByDefault;
    }

    public void setMercury(Mercury mercury) {
        this.mercury = mercury;
    }

    public void setConfig(AbstractModuleProperties config) {
        this.config = config;
    }

    public AbstractModuleProperties getConfig() {
        return config;
    }
}
