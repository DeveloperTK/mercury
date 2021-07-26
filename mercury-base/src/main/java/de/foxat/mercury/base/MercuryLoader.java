package de.foxat.mercury.base;

import de.foxat.mercury.base.config.BaseConfigField;
import de.foxat.mercury.base.config.MercuryConfig;
import de.foxat.mercury.base.discord.MercurySystem;
import de.foxat.mercury.base.modules.ModuleDisableTask;
import de.foxat.mercury.base.modules.ModuleLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;

public class MercuryLoader {

    private static final Logger logger = LogManager.getLogger(MercuryLoader.class);

    public static void main(String[] args) {
        logger.info("Launching MercuryLoader");

        // Instantiate private singleton instance
        MercuryConfig.getInstance();
        // Start the system
        MercurySystem system = new MercurySystem(MercuryConfig.getProperty(BaseConfigField.DISCORD_TOKEN));

        ModuleLoader moduleLoader = new ModuleLoader(new File("modules/"), List.of());
        ModuleDisableTask.addShutdownHook(moduleLoader);

        moduleLoader.loadModules();
        moduleLoader.enableLoadedModules();
    }

}
