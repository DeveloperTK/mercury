package de.foxat.mercury.mm;

import de.foxat.mercury.api.MercuryModule;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public class MassMover extends MercuryModule {

    @Override
    protected void onLoad() {
        getMercury().getCommandRegistry().registerCommand(this,
                new CommandData("massmove", "Moves multiple people at once")
        , false);
    }

    @Override
    protected void onEnable() {

    }

    @Override
    protected void onDisable() {

    }

    @Override
    protected void onCommand(SlashCommandEvent slashCommandEvent) {
        slashCommandEvent.reply("Command handler from module").queue();
    }
}
