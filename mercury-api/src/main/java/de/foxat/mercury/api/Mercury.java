package de.foxat.mercury.api;

import de.foxat.mercury.api.audio.GuildAudioManager;
import de.foxat.mercury.api.command.CommandRegistry;
import de.foxat.mercury.api.config.MercuryConfig;
import de.foxat.mercury.api.event.MercuryScheduler;
import de.foxat.mercury.api.tasks.TaskScheduler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.List;

public interface Mercury {

    MercuryConfig getConfig();

    JDA getRootInstance();

    List<JDA> getInstances();

    JDA getInstanceByName(String name);

    JDA getInstanceById(String name);

    void addRootListener(ListenerAdapter listenerAdapters);

    void removeRootListener(ListenerAdapter listenerAdapters);

    MercuryScheduler getScheduler();

    CommandRegistry getCommandRegistry();

    GuildAudioManager getAudioManager();

    TaskScheduler newRoundRobinScheduler(String guildId);

    default TaskScheduler newRoundRobinScheduler(long guildId) {
        return newRoundRobinScheduler(String.valueOf(guildId));
    }

}
