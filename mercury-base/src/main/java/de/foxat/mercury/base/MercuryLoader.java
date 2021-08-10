package de.foxat.mercury.base;

import de.foxat.mercury.api.Mercury;
import de.foxat.mercury.api.config.MercuryConfig;
import de.foxat.mercury.base.config.XMLMercuryConfig;
import de.foxat.mercury.base.discord.MercurySystem;
import de.foxat.mercury.base.modules.ModuleDisableTask;
import de.foxat.mercury.base.modules.ModuleLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;

public class MercuryLoader {

    private static final Logger logger = LogManager.getLogger(MercuryLoader.class);

    private static MercuryConfig config;

    public static void main(String[] args) {
        logger.info("Launching MercuryLoader");

        // Instantiate private singleton instance
        XMLMercuryConfig.getInstance();

        // Start the system
        MercurySystem system = new MercurySystem();
        system.startup();

        // initialize the mercury instance
        ModuleLoader moduleLoader = new ModuleLoader(new File("modules/"), List.of());
        Mercury mercury = new MercuryImpl(system, moduleLoader);

        // load modules

        moduleLoader.setMercury(mercury);
        ModuleDisableTask.addShutdownHook(moduleLoader);
        moduleLoader.loadModules();
        moduleLoader.enableLoadedModules();
    }

}
