package de.foxat.mercury.api.command;

import de.foxat.mercury.api.MercuryModule;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

public interface CommandRegistry {

    void registerCommand(MercuryModule module, CommandData commandData, boolean global);

}
