package de.foxat.mercury.mm;

import net.dv8tion.jda.api.events.interaction.SelectionMenuEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class MusicInteractionListener extends ListenerAdapter {

    private Music parent;
    private Map<String, String> interactionMap;

    public MusicInteractionListener(Music parent) {
        this.parent = parent;
        this.interactionMap = new HashMap<>();
    }

    @Override
    public void onSelectionMenu(@NotNull SelectionMenuEvent event) {
        if (event.getComponent() == null) return;

        if (interactionMap.containsKey(event.getComponent().getId())) {
            handleInteraction(event);
        } else {
            event.reply("This interaction expired!").queue();
        }
    }

    private void handleInteraction(SelectionMenuEvent event) {
        if (event.getMember() == null
                || event.getMember().getVoiceState() == null
                || event.getMember().getVoiceState().getChannel() == null) {
            event.reply("Error: this interaction expired.").setEphemeral(true).queue();
        }
    }

}
