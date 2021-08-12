package de.foxat.mercury.mm;

import de.foxat.mercury.api.MercuryModule;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public class Music extends MercuryModule {

    private MusicCommandHandler commandHandler;

    @Override
    protected void onLoad() {
        commandHandler = new MusicCommandHandler(this);
        getMercury().getCommandRegistry().registerCommand(this,
                new CommandData("music", "Play music in discord voice channels").addSubcommands(
                        new SubcommandData("join", "Summon a music bot to your channel"),
                        new SubcommandData("leave", "Kicks the music bot from your channel"),
                        new SubcommandData("play", "Plays a URL in your channel").addOptions(
                                new OptionData(OptionType.STRING, "url", "target url", true)
                        ),
                        new SubcommandData("youtube", "Searches and plays a YouTube video").addOptions(
                                new OptionData(OptionType.STRING, "query", "Your search query", true)
                        )
                )
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
        commandHandler.onCommand(slashCommandEvent);
    }
}
