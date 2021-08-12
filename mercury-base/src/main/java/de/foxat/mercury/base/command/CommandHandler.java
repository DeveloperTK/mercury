package de.foxat.mercury.base.command;

import de.foxat.mercury.api.MercuryModule;
import de.foxat.mercury.api.command.CommandRegistry;
import de.foxat.mercury.api.config.ModulePropertyField;
import de.foxat.mercury.base.modules.ModuleLoader;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandHandler extends ListenerAdapter implements CommandRegistry {

    private static final Logger logger = LogManager.getLogger(CommandHandler.class.getSimpleName());

    private final Map<String, String> commands;
    private final List<CommandData> guildCommands;

    private final ModuleLoader moduleLoader;
    private final JDA rootInstance;

    public CommandHandler(ModuleLoader moduleLoader, JDA rootInstance) {
        commands = new HashMap<>();
        guildCommands = new ArrayList<>();
        this.rootInstance = rootInstance;
        this.moduleLoader = moduleLoader;
    }

    public void registerCommand(MercuryModule module, CommandData commandData, boolean global) {
        if (commands.containsKey(commandData.getName())) {
            throw new IllegalArgumentException("Command " + commandData.getName()
                    + " already registered by module " + commands.get(commandData.getName()));
        }

        commands.put(commandData.getName(), module.getConfig().getProperty(ModulePropertyField.NAME));

        if (global) {
            rootInstance.upsertCommand(commandData).queue();
        } else {
            rootInstance.getGuilds().forEach(guild -> guild.upsertCommand(commandData).queue());
            guildCommands.add(commandData);
        }

        logger.info("Registered command {} in module {}",
                commandData.getName(),
                module.getConfig().getProperty(ModulePropertyField.NAME)
        );
    }

    @Override
    public void onSlashCommand(@NotNull SlashCommandEvent event) {
        if (commands.containsKey(event.getName())) {
            MercuryModule module = moduleLoader.getModule(commands.get(event.getName()));

            logger.info("Dispatched command {} to module {}",
                    event.getName(),
                    module.getConfig().getProperty(ModulePropertyField.NAME)
            );

            module.dispatchCommandEvent(event);
        }
    }

    @Override
    public void onGuildJoin(@NotNull GuildJoinEvent event) {
        for (CommandData guildCommand : guildCommands) {
            event.getGuild().upsertCommand(guildCommand).queue();
        }
    }
}
