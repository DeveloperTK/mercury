package de.foxat.mercury.base;

import de.foxat.mercury.base.config.BaseConfigField;
import de.foxat.mercury.base.config.MercuryConfig;
import de.foxat.mercury.base.discord.System;

public class MercuryLoader {

    public static void main(String[] args) {
        // Instantiate private singleton instance
        MercuryConfig.getInstance();
        // Start the system
        System system = new System(MercuryConfig.getProperty(BaseConfigField.DISCORD_TOKEN));
    }

}
