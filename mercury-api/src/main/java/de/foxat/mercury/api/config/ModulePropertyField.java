package de.foxat.mercury.api.config;

public enum ModulePropertyField {
    NAME,
    VERSION,
    AUTHOR,
    MAIN_CLASS,

    DESCRIPTION("No description provided"),
    ENABLED("true")

    ;

    private final boolean optional;
    private final String defaultValue;

    ModulePropertyField() {
        optional = false;
        defaultValue = "";
    }

    ModulePropertyField(String defaultValue) {
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
