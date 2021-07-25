package de.foxat.mercury.base.config;

import java.io.*;
import java.util.Properties;

public class MercuryConfig {

    private static MercuryConfig instance;

    private final Properties properties;

    private MercuryConfig() throws IOException {
        this.properties = new Properties();
        properties.load(new FileInputStream("config/discord-base.properties"));

        if (!isValidConfig()) {
            throw new IllegalStateException("discord-base.properties config is missing required parameters");
        }
    }

    /**
     * Checks whether the config file is valid
     *
     * @return whether the config file is valid
     */
    private boolean isValidConfig() {
        for (BaseConfigField value : BaseConfigField.values()) {
            if (!value.isOptional() && !properties.containsKey(value.name())) {
                return false;
            }
        }

        return true;
    }

    /**
     * Singleton getter for the config class
     *
     * @return singleton instance if existent
     */
    public static MercuryConfig getInstance() {
        if (instance == null) {
            try {
                instance = new MercuryConfig();
            } catch (FileNotFoundException exception) {
                createConfig();
                throw new IllegalStateException("Please configure config/discord-base.properties", exception);
            } catch (IOException exception) {
                throw new IllegalStateException("IOException while loading discord-base.properties config", exception);
            }
        }

        return instance;
    }

    /**
     * Try writing a configuration file with all required fields
     *
     * @throws Error the file could not be created
     */
    private static void createConfig() {
        try (FileWriter writer = new FileWriter("config/discord-base.properties");) {
            for (BaseConfigField field : BaseConfigField.values()) {
                writer.append(field.name()).append("=");

                if (field.isOptional()) {
                    writer.append(field.getDefaultValue());
                } else {
                    writer.append("change_me");
                }

                writer.append(System.lineSeparator());
            }

            writer.flush();
        } catch (IOException exception) {
            throw new Error("Missing File: config/discord-base.properties which could not be created as well! "
                    + "(maybe check your permissions)", exception);
        }
    }

    /**
     * Getter method for the internal {@link Properties} object
     * @return raw {@link Properties} object
     */
    public Properties getProperties() {
        return properties;
    }

    /**
     * Get a field from the configuration file
     *
     * @param field property field to get
     * @return config or default value
     * @throws IllegalArgumentException if the field is null
     */
    public static String getProperty(BaseConfigField field) {
        if (field == null) {
            throw new IllegalArgumentException("field can not be null");
        }

        if (getInstance().getProperties().containsKey(field.name())) {
            return getInstance().getProperties().getProperty(field.name());
        } else {
            return field.getDefaultValue();
        }
    }
}
