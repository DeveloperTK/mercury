package de.foxat.mercury.api.config;

public enum ModuleConfigField {
    NAME,
    VERSION,
    AUTHOR,
    MAIN_CLASS,

    DESCRIPTION("No description provided"),
    ENABLED("true")

    ;

    private final boolean optional;
    private final String defaultValue;

    ModuleConfigField() {
        optional = false;
        defaultValue = "";
    }

    ModuleConfigField(String defaultValue) {
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
