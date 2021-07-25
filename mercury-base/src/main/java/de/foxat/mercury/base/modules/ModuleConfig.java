package de.foxat.mercury.base.modules;

import de.foxat.mercury.api.config.AbstractModuleConfig;
import de.foxat.mercury.api.config.ModuleConfigField;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Properties;

public class ModuleConfig extends AbstractModuleConfig {

    private final Properties properties;

    public ModuleConfig(InputStreamReader reader) throws IOException {
        this.properties = new Properties();
        properties.load(reader);

        if (!isValidConfig()) {
            throw new IllegalStateException("module config is missing required parameters");
        }
    }

    /**
     * Checks whether the config file is valid
     *
     * @return whether the config file is valid
     */
    private boolean isValidConfig() {
        for (ModuleConfigField value : ModuleConfigField.values()) {
            if (!value.isOptional() && !properties.containsKey(value.name())) {
                return false;
            }
        }

        return true;
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
    public String getProperty(ModuleConfigField field) {
        if (field == null) {
            throw new IllegalArgumentException("field can not be null");
        }

        if (getProperties().containsKey(field.name())) {
            return getProperties().getProperty(field.name());
        } else {
            return field.getDefaultValue();
        }
    }

}
