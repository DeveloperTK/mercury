package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeSearchProvider;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import de.foxat.mercury.api.audio.AudioPlayerSendHandler;
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

    private final Music parent;
    private final MusicInteractionListener interactionListener;

    private final Map<String, AudioConnection> playerMap;
    private final AudioPlayerManager playerManager;

    private final YoutubeSearchProvider youtubeSearchProvider;
    private final YoutubeAudioSourceManager youtubeAudioSourceManager;

    public MusicCommandHandler(Music parent) {
        this.parent = parent;
        this.interactionListener = parent.getInteractionListener();

        this.playerMap = new HashMap<>();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);

        youtubeSearchProvider = new YoutubeSearchProvider();
        youtubeAudioSourceManager = new YoutubeAudioSourceManager();
    }

    AudioPlayer createPlayer(String guildId, String applicationId) {
        AudioPlayer player = playerManager.createPlayer();
        player.setVolume((int) (player.getVolume() * VOLUME_REDUCTION));
        playerMap.put(guildId + applicationId, new AudioConnection(player));
        return player;
    }

    void removePlayer(String guildId, String applicationId) {
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
                pause(event, channel);
                return;
            case "skip":
                skip(event, channel);
                return;
            default:
                event.reply("Error: Subcommand not found!").queue();
        }
    }

    AudioPlayer createAndConnect(VoiceChannel channel, String guildId, String instanceId) {
        AudioManager audioManager = Objects.requireNonNull(parent.getMercury().getInstanceById(instanceId)
                .getGuildById(guildId)).getAudioManager();
        audioManager.openAudioConnection(channel);
        AudioPlayer audioPlayer = createPlayer(guildId, instanceId);
        audioManager.setSendingHandler(new AudioPlayerSendHandler(audioPlayer));
        return audioPlayer;
    }

    String findApplication(VoiceChannel channel) {
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

    AudioConnection connectOrGetConnection(SlashCommandEvent event, VoiceChannel channel) {
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
                if (connection.enqueueTrack(audioTrack)) {
                    event.reply("Queued **" + audioTrack.getInfo().title + "**").queue();
                } else {
                    event.reply("Playing **" + audioTrack.getInfo().title + "**").queue();
                }
            }

            @Override
            public void playlistLoaded(AudioPlaylist audioPlaylist) {
                parent.getLogger().info("Loaded playlist " + audioPlaylist.getName());

                int selected = audioPlaylist.getTracks().indexOf(audioPlaylist.getSelectedTrack());
                for (int i = selected; i < audioPlaylist.getTracks().size(); i++) {
                    connection.enqueueTrack(audioPlaylist.getTracks().get(i));
                }

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

    private void pause(SlashCommandEvent event, VoiceChannel channel) {
        String instanceId = findApplication(channel);

        if (instanceId == null) {
            event.reply("Cannot find a music instance to pause").queue();
        }

        // toggle paused state
        AudioPlayer audioPlayer = playerMap.get(event.getGuild().getId() + instanceId).getAudioPlayer();

        if (audioPlayer.getPlayingTrack() == null
                || audioPlayer.getPlayingTrack().getState().equals(AudioTrackState.INACTIVE)
                || audioPlayer.getPlayingTrack().getState().equals(AudioTrackState.FINISHED)) {
            event.reply("There is nothing playing at the moment").queue();
        }

        if (audioPlayer.isPaused()) {
            audioPlayer.setPaused(false);
            event.reply("Resumed the current track").queue();
        } else {
            audioPlayer.setPaused(true);
            event.reply("Paused the current track").queue();
        }
    }

    private void skip(SlashCommandEvent event, VoiceChannel channel) {
        String instanceId = findApplication(channel);

        if (instanceId == null) {
            event.reply("Cannot find a music instance to pause").queue();
        }

        // toggle paused state
        AudioConnection connection = playerMap.get(event.getGuild().getId() + instanceId);
        AudioPlayer audioPlayer = connection.getAudioPlayer();

        if (audioPlayer.getPlayingTrack() == null
                || audioPlayer.getPlayingTrack().getState().equals(AudioTrackState.INACTIVE)
                || audioPlayer.getPlayingTrack().getState().equals(AudioTrackState.FINISHED)) {
            event.reply("There is nothing playing at the moment.").queue();
        }

        audioPlayer.stopTrack();

        synchronized (connection.getTrackQueue()) {
            if (connection.getTrackQueue().isEmpty()) {
                event.reply("Skipped current track.").queue();
            } else {
                AudioTrack newTrack = connection.getTrackQueue().poll();
                audioPlayer.playTrack(newTrack);

                event.reply("Skipped current track. Playing **" + newTrack.getInfo().title + "**").queue();
            }
        }
    }

}
