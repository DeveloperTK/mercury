package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchProvider;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import de.foxat.mercury.api.MercuryModule;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectionMenu;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MusicCommandHandler {

    private static final float VOLUME_REDUCTION = 0.5f;

    private final MercuryModule parent;
    private final Map<String, AudioConnection> playerMap;
    private final AudioPlayerManager playerManager;

    private final YoutubeSearchProvider youtubeSearchProvider;
    private final YoutubeAudioSourceManager youtubeAudioSourceManager;

    public MusicCommandHandler(MercuryModule parent) {
        this.parent = parent;
        this.playerMap = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);

        youtubeSearchProvider = new YoutubeSearchProvider();
        youtubeAudioSourceManager = new YoutubeAudioSourceManager();
    }

    private AudioPlayer createPlayer(String guildId, String applicationId) {
        AudioPlayer player = playerManager.createPlayer();
        player.setVolume((int) (player.getVolume() * VOLUME_REDUCTION));
        playerMap.put(guildId + applicationId, new AudioConnection(player));
        return player;
    }

    private void removePlayer(String guildId, String applicationId) {
        this.playerMap.remove(guildId + applicationId);
    }

    public void onCommand(SlashCommandEvent event) {
        VoiceChannel channel;
        try {
            channel = Objects.requireNonNull(Objects.requireNonNull(Objects.requireNonNull(
                                event.getMember()).getVoiceState()).getChannel());
        } catch (NullPointerException exception) {
            event.reply("You need to be in a voice channel in order to executed this command!").queue();
            return;
        }

        assert event.getSubcommandName() != null;
        switch (event.getSubcommandName()) {
            case "join":
                join(event, channel);
                return;
                // TODO: handle moving clients to different channels
            case "leave":
                leave(event, channel);
                return;
            case "play":
                play(event, channel);
                return;
            case "youtube":
                youtube(event, channel);
                return;
            case "pause":

                return;
            default:
                event.reply("Error: Subcommand not found!").queue();
        }
    }

    private AudioPlayer createAndConnect(VoiceChannel channel, String guildId, String instanceId) {
        AudioManager audioManager = Objects.requireNonNull(parent.getMercury().getInstanceById(instanceId)
                .getGuildById(guildId)).getAudioManager();
        audioManager.openAudioConnection(channel);
        AudioPlayer audioPlayer = createPlayer(guildId, instanceId);
        audioManager.setSendingHandler(new AudioPlayerSendHandler(audioPlayer));
        return audioPlayer;
    }

    private String findApplication(VoiceChannel channel) {
        for (Member member : channel.getMembers()) {
            for (JDA instance : parent.getMercury().getInstances()) {
                if (instance.getSelfUser().getId().equals(member.getId())) {
                    return member.getId();
                }
            }
        }

        return null;
    }

    private void join(SlashCommandEvent event, VoiceChannel channel) {
        try {
            String instanceId = parent.getMercury().getAudioManager()
                    .getOrAcquireInstanceId(event.getGuild().getId(), channel.getId());

            try {
                createAndConnect(channel, event.getGuild().getId(), instanceId);
                event.reply("Joined channel " + channel.getAsMention()).queue();
            } catch (NullPointerException exception) {
                parent.getLogger().error("Guild " + event.getGuild().getName() + " - "
                        + event.getGuild().getId() + " was null on instance " + instanceId, exception);
                event.reply("An error occurred while joining channel" + channel.getAsMention() + "!").queue();
            }
        } catch (IndexOutOfBoundsException exception) {
            event.reply("There are no more instances available for this server,"
                    + " you will need to invite more!").queue();
        }
    }

    private void leave(SlashCommandEvent event, VoiceChannel channel) {
        try {
            String instanceId = parent.getMercury().getAudioManager().release(event.getGuild().getId(), channel.getId());
            Objects.requireNonNull(parent.getMercury().getInstanceById(instanceId).getGuildById(channel.getGuild().getId()))
                    .getAudioManager().closeAudioConnection();
            removePlayer(event.getGuild().getId(), channel.getId());
            event.reply("Left the channel " + channel.getAsMention()).queue();
        } catch (IllegalArgumentException exception) {
            event.reply("I cannot find any music bot connected to your channel").queue();
        }
    }

    private AudioConnection connectOrGetConnection(SlashCommandEvent event, VoiceChannel channel) {
        String instanceId = findApplication(channel);
        String key = event.getGuild().getId() + instanceId;

        if (playerMap.containsKey(key)) {
            return playerMap.get(key);
        } else {
            instanceId = parent.getMercury().getAudioManager()
                    .getOrAcquireInstanceId(event.getGuild().getId(), channel.getId());
            createAndConnect(channel, event.getGuild().getId(), instanceId);
            return playerMap.get(event.getGuild().getId() + instanceId);
        }
    }

    private void play(SlashCommandEvent event, VoiceChannel channel) {
        final AudioConnection connection;

        try {
            connection = connectOrGetConnection(event, channel);
        } catch (NullPointerException | IndexOutOfBoundsException exception) {
            event.reply("There are no more instances available for this server,"
                    + " you will need to invite more!").queue();
            return;
        }

        playerManager.loadItem(event.getOption("url").getAsString(), new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack audioTrack) {
                parent.getLogger().info("Loaded new audio track " + audioTrack.getInfo().title);
                connection.enqueueTrack(audioTrack);
                event.reply("Playing **" + audioTrack.getInfo().title + "**").queue();
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                parent.getLogger().info("Loaded playlist " + audioPlaylist.getName());
                connection.enqueueTrack(audioPlaylist.getSelectedTrack());
                event.reply("Loaded playlist **" + audioPlaylist.getName() + "**").queue();
            }

            @Override
            public void noMatches() {
                event.reply("Track not found").queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                event.reply("Track loading failed!").queue();
                parent.getLogger().error("An exception was thrown while loading track", e);
            }
        });
    }

    private void youtube(SlashCommandEvent event, VoiceChannel channel) {
        final AudioConnection connection;

        try {
            connection = connectOrGetConnection(event, channel);
        } catch (NullPointerException | IndexOutOfBoundsException exception) {
            event.reply("There are no more instances available for this server,"
                    + " you will need to invite more!").queue();
            return;
        }

        String query = event.getOption("query").getAsString();

        AudioItem audioItem = youtubeSearchProvider.loadSearchResult(query.strip(), audioTrackInfo ->
                new YoutubeAudioTrack(audioTrackInfo, youtubeAudioSourceManager));

        if (audioItem instanceof BasicAudioPlaylist) {
            BasicAudioPlaylist playlist = (BasicAudioPlaylist) audioItem;

            SelectionMenu.Builder menuBuilder = SelectionMenu.create("menu:" + UUID.randomUUID())
                    .setPlaceholder("Select a search result...")
                    .setRequiredRange(1, 1);

            playlist.getTracks().forEach(track -> menuBuilder.addOption(track.getInfo().title, track.getIdentifier(), track.getInfo().author));

            event.reply("Search results for: **" + query + "**\n" + "INTERACTION LISTENER IS MISSING!!!!!!").addActionRow(menuBuilder.build()).setEphemeral(true).queue();

        } else {
            event.reply("Could not find any videos for query: **" + query + "**").queue();
        }
    }

}
