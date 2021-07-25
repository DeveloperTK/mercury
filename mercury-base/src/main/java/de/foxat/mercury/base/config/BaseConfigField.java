package de.foxat.mercury.base.config;

public enum BaseConfigField {
    DISCORD_TOKEN,
    HOME_GUILD(null),
    BOT_OWNERS(null)

    ;

    private final boolean optional;
    private final String defaultValue;

    BaseConfigField() {
        optional = false;
        defaultValue = "";
    }

    BaseConfigField(String defaultValue) {
        optional = true;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns true if the parameter is optional
     *
     * @return true if the parameter is optional
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Returns the default parameter value
     * @return the default parameter value
     */
    public String getDefaultValue() {
        return defaultValue;
    }
}
