package de.foxat.mercury.base.modules;

import de.foxat.mercury.api.Mercury;
import de.foxat.mercury.api.MercuryModule;
import de.foxat.mercury.api.config.ModulePropertyField;
import de.foxat.mercury.base.MercuryLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * @author Christian Schliz
 * @version 1.0
 * */
public class ModuleLoader {

    public static final String LOGGER_NAME = "ModuleLoader";

    private final File localModuleDirectory;
    private final File[] remoteModuleDirectories;
    private final Map<String, MercuryModule> modules;

    private final Logger logger;

    private Mercury mercury;

    /**
     * The ModuleManager is responsible for loading, enabling
     * and disabling all modules. It searches the local module
     * directory in the plugin folder, but can also retrieve
     * modules from your network.
     *
     * @param localModuleDirectory    The path to the local directory.
     * @param remoteModuleDirectories A list of remote directories on your drive
     *                                or your network. modules from there are loaded
     *                                but not enabled by default and have to be explicitly
     *                                set to be enabled via the {@code module.properties} file
     * */
    public ModuleLoader(File localModuleDirectory, List<File> remoteModuleDirectories) {
        this.localModuleDirectory = localModuleDirectory;
        this.remoteModuleDirectories = remoteModuleDirectories.toArray(new File[0]);
        modules = new HashMap<>();
        logger = LogManager.getLogger(LOGGER_NAME);
    }

    /**
     * Cannot be constructor parameter because of cyclic dependencies
     * @param mercury mercury system
     */
    public void setMercury(Mercury mercury) {
        this.mercury = mercury;
    }

    /**
     * Only searches the modules directory inside
     * the plugin folder and loads them.
     * */
    @SuppressWarnings("unused")
    public void loadLocalModules() {
        registerModulesFromDirectories(this.localModuleDirectory);
    }

    /**
     * Enables all modules loaded into {@link ModuleLoader#modules}.
     * */
    public void enableLoadedModules() {
        modules.forEach((name, module) -> {
            logger.info("Enabling module {} version {} by {}.", name,
                    module.getConfig().getProperty(ModulePropertyField.VERSION),
                    module.getConfig().getProperty(ModulePropertyField.AUTHOR));
            module.tryEnable();
        });
    }

    /**
     * Disables all modules loaded into {@link ModuleLoader#modules}.
     * */
    public void disableLoadedModules() {
        modules.forEach((name, module) -> {
            logger.info("Disabling module {} version {} by {}.", name,
                    module.getConfig().getProperty(ModulePropertyField.VERSION),
                    module.getConfig().getProperty(ModulePropertyField.AUTHOR));
            module.doDisable();
        });
    }

    /**
     * Looks for modules in local and remote directories
     * and loads all modules found.
     * */
    @SuppressWarnings("unused")
    public void loadModules() {
        if (Objects.nonNull(this.localModuleDirectory)) {
            logger.info("Loading modules from local directory");
            registerModulesFromDirectories(this.localModuleDirectory);
        }

        if (Objects.nonNull(this.remoteModuleDirectories)) {
            logger.info("Loading modules from remote directories");
            for (File directory : this.remoteModuleDirectories) {
                logger.info("Checking out remote directory - {}", directory.getPath());
                registerModulesFromDirectories(directory);
            }
        }
    }

    /**
     * Returns a MercuryModule by its name. Used for
     * Interaction between modules or via the SpigotMS
     * command tools.
     *
     * @param name The unique name of a module.
     * @return MercuryModule module with the given name.
     * */
    @SuppressWarnings("unused")
    public MercuryModule getModule(String name) {
        if (this.modules.containsKey(name)) {
            return this.modules.get(name);
        } else {
            System.err.printf("No such module named %s found %n", name);
            return null;
        }
    }

    // -- private methods

    @SuppressWarnings("checkstyle:IllegalCatch")
    private void registerModulesFromDirectories(final File directory) {
        URL[] urls = new URL[0];

        if (Objects.isNull(directory)) {
            // No directory specified
            logger.error("Cannot load modules from directory: null");
        } else if (!directory.canRead()) {
            // Can't read from the specified path
            logger.error("Cannot load modules from a read protected path!");
        } else if (directory.isDirectory()) {
            // Load all files within the specified directory
            List<File> files = new ArrayList<>(
                    Arrays.asList(
                            Objects.requireNonNull(directory.listFiles())
                    )
            );
            files.removeIf(f -> !f.getName().endsWith(".jar"));

            urls = new URL[files.size()];

            for (int i = 0; i < files.size(); i++) {
                try {
                    urls[i] = files.get(i).toURI().toURL();
                } catch (MalformedURLException | NullPointerException e) {
                    e.printStackTrace();
                }
            }
        } else if (directory.isFile() && directory.getName().endsWith(".jar")) {
            // Only load one jar file
            try {
                urls = new URL[]{directory.toURI().toURL()};
            } catch (MalformedURLException exception) {
                exception.printStackTrace();
            }
        } else {
            logger.error("Module at {} was neither a directory not in .jar format!", directory.getPath());
        }

        loadJARsFromURLIntoClasspath(urls);
    }

    private void loadJARsFromURLIntoClasspath(URL[] urls) {
        try {
            // Load all .jar files from the target directory into the current classpath
            URLClassLoader moduleClassLoader = new URLClassLoader(urls, MercuryLoader.class.getClassLoader());

            // A list of all module.properties config files found inside the directory
            Enumeration<URL> configPaths = moduleClassLoader.findResources("module.properties");

            if (!configPaths.hasMoreElements()) {
                logger.error("No module.properties config files found. Please check your jar contents.");
            }

            while (configPaths.hasMoreElements()) {
                URL nextUrl = configPaths.nextElement();

                // Read the file
                InputStreamReader configFileStream = new InputStreamReader(nextUrl.openStream());

                try {
                    ModuleProperties moduleProperties = new ModuleProperties(configFileStream);
                    initializeModule(moduleClassLoader, moduleProperties);
                } catch (IllegalStateException exception) {
                    logger.error("Found invalid module configuration inside " + nextUrl.getPath(), exception);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void initializeModule(URLClassLoader moduleClassLoader, ModuleProperties config) {
        try {
            // Load the target class
            Class<?> testModuleClass = Class.forName(config.getProperty(ModulePropertyField.MAIN_CLASS),
                    true, moduleClassLoader);

            // Create an instance of the targeted class
            MercuryModule moduleInstance = (MercuryModule)
                    testModuleClass.getDeclaredConstructor().newInstance();

            moduleInstance.setEnabledByDefault(Boolean.parseBoolean(config.getProperty(ModulePropertyField.ENABLED)));
            moduleInstance.setConfig(config);
            moduleInstance.setMercury(mercury);

            // Add the module to the list
            modules.put(config.getProperty(ModulePropertyField.NAME), moduleInstance);

            moduleInstance.doLoad();
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException
                | IllegalAccessException | ClassNotFoundException exception) {

            // Could either not cast to MercuryModule or the jar is obfuscated
            logger.error("Could not load module at " + config.getProperty(ModulePropertyField.MAIN_CLASS), exception);
            exception.printStackTrace();
        }

    }

    /**
     * @return local module directory
     */
    public File getLocalModuleDirectory() {
        return localModuleDirectory;
    }

    /**
     * @return remote module directories
     */
    public File[] getRemoteModuleDirectories() {
        return remoteModuleDirectories;
    }

    /**
     * @return map of all modules
     */
    public Map<String, MercuryModule> getModules() {
        return modules;
    }
}