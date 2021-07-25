package de.foxat.mercury.base.modules;

import de.foxat.mercury.api.MercuryModule;
import de.foxat.mercury.api.config.ModuleConfigField;
import de.foxat.mercury.base.MercuryLoader;

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

    private final File localModuleDirectory;
    private final File[] remoteModuleDirectories;
    private final Map<String, MercuryModule> modules;

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
            System.out.println("[SpigotMS] Enabling module " + name + " at " + module.getClass().getCanonicalName());
            module.tryEnable();
        });
    }

    /**
     * Disables all modules loaded into {@link ModuleLoader#modules}.
     * */
    public void disableLoadedModules() {
        modules.forEach((name, module) -> {
            System.out.println("[SpigotMS] Disabling module " + name + " at " + module.getClass().getCanonicalName());
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
            registerModulesFromDirectories(this.localModuleDirectory);
        }

        if (Objects.nonNull(this.remoteModuleDirectories)) {
            for (File directory : this.remoteModuleDirectories) {
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
            System.out.println("Cannot load modules from File: null");
        } else if (!directory.canRead()) {
            // Can't read from the specified path
            System.out.println("Cannot load modules from a read protected path!");
        } else if (directory.isDirectory()) {
            // Load all files within the specified directory
            ArrayList<File> files = new ArrayList<>(
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
            System.out.println("modules must be in the .jar format");
        }

        loadJARsFromURLIntoClasspath(urls);
    }

    private void loadJARsFromURLIntoClasspath(URL[] urls) {
        try {
            // Load all .jar files from the target directory into the SpigotMS classpath
            URLClassLoader moduleClassLoader = new URLClassLoader(urls, MercuryLoader.class.getClassLoader());

            // A list of all module.yml config files found inside the directory
            Enumeration<URL> configPaths = moduleClassLoader.findResources("module.properties");

            if (configPaths.hasMoreElements()) {
                // Read the file
                InputStreamReader configFileStream = new InputStreamReader(configPaths.nextElement().openStream());
                ModuleConfig moduleConfig = new ModuleConfig(configFileStream);

            } else {
                // TODO: Error Message - no module config found
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    public void initializeModule(URLClassLoader moduleClassLoader, ModuleConfig config) {
        try {
            // Load the target class
            Class<?> testModuleClass = Class.forName(config.getProperty(ModuleConfigField.MAIN_CLASS),
                    true, moduleClassLoader);

            // Create an instance of the targeted class
            MercuryModule moduleInstance = (MercuryModule)
                    testModuleClass.getDeclaredConstructor().newInstance();

            moduleInstance.setEnabledByDefault(Boolean.parseBoolean(config.getProperty(ModuleConfigField.ENABLED)));
            moduleInstance.setConfig(config);

            // Add the module to the list
            modules.put(config.getProperty(ModuleConfigField.NAME), moduleInstance);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException exception) {
            // Could either not cast to MercuryModule or the jar is obfuscated
            exception.printStackTrace();
        } catch (IllegalAccessException | ClassNotFoundException exception) {
            // bad jar
            System.err.println("Please check your configuration files"); // TODO: Error Message - Please check your configuration files
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