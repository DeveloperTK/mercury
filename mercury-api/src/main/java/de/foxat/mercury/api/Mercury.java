package de.foxat.mercury.api;

import de.foxat.mercury.api.command.CommandRegistry;
import de.foxat.mercury.api.config.MercuryConfig;
import de.foxat.mercury.api.event.MercuryScheduler;
import de.foxat.mercury.api.tasks.TaskScheduler;
import net.dv8tion.jda.api.JDA;

import java.util.List;

public interface Mercury {

    MercuryConfig getConfig();

    JDA getRootInstance();

    List<JDA> getInstances();

    JDA getInstanceByName(String name);

    JDA getInstanceById(String name);

    MercuryScheduler getScheduler();

    TaskScheduler newRoundRobinScheduler(String guildId);

    CommandRegistry getCommandRegistry();

    default TaskScheduler newRoundRobinScheduler(long guildId) {
        return newRoundRobinScheduler(String.valueOf(guildId));
    }

}
