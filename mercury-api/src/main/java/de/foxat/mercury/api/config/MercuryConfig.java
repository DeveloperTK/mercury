package de.foxat.mercury.api.config;

import java.util.List;

public interface MercuryConfig {

    String getSystemName();

    String getMainInstanceId();

    boolean isLazyLoaded();

    boolean hasHomeGuild();

    String getHomeGuildId();

    DiscordInstance getRootInstance();

    List<DiscordInstance> getInstances();

}
