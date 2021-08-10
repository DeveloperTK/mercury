package de.foxat.mercury.base;

import de.foxat.mercury.api.Mercury;
import de.foxat.mercury.api.MercuryModule;
import de.foxat.mercury.api.command.CommandRegistry;
import de.foxat.mercury.api.config.DiscordInstance;
import de.foxat.mercury.api.config.MercuryConfig;
import de.foxat.mercury.api.event.MercuryScheduler;
import de.foxat.mercury.api.tasks.TaskScheduler;
import de.foxat.mercury.base.command.CommandHandler;
import de.foxat.mercury.base.config.XMLMercuryConfig;
import de.foxat.mercury.base.discord.MercurySystem;
import de.foxat.mercury.base.event.MercurySchedulerImpl;
import de.foxat.mercury.base.modules.ModuleLoader;
import de.foxat.mercury.base.tasks.RoundRobinTaskScheduler;
import net.dv8tion.jda.api.JDA;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 */
public class MercuryImpl implements Mercury {

    private final MercurySystem mercurySystem;
    private final MercuryScheduler mercuryScheduler;
    private final CommandHandler commandHandler;

    public MercuryImpl(MercurySystem mercurySystem, ModuleLoader moduleLoader) {
        this.mercurySystem = mercurySystem;
        this.mercuryScheduler = new MercurySchedulerImpl();
        this.commandHandler = new CommandHandler(moduleLoader, getRootInstance());

        getRootInstance().addEventListener(commandHandler);
    }

    @Override
    public MercuryConfig getConfig() {
        return XMLMercuryConfig.getInstance();
    }

    @Override
    public JDA getRootInstance() {
        return mercurySystem.getInstances().get(getConfig().getRootInstance().getId());
    }

    @Override
    public List<JDA> getInstances() {
        return new ArrayList<>(mercurySystem.getInstances().values());
    }

    @Override
    public JDA getInstanceByName(String name) {
        return mercurySystem.getInstances().get(name);
    }

    @Override
    public JDA getInstanceById(String id) {
        return mercurySystem.getInstances().values().stream()
                .filter(jda -> jda.getSelfUser().getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    @Override
    public MercuryScheduler getScheduler() {
        return mercuryScheduler;
    }

    @Override
    public TaskScheduler newRoundRobinScheduler(String guildId) {
        return new RoundRobinTaskScheduler(guildId, this);
    }

    @Override
    public CommandRegistry getCommandRegistry() {
        return commandHandler;
    }

}
