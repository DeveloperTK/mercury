package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.foxat.mercury.api.audio.AudioPlayerSendHandler;
import de.foxat.mercury.api.config.DiscordInstance;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

public class RadioInstance {

    private final Logger logger;

    private final String homeGuildId;
    private final DiscordInstance discordInstance;
    private final JDA jda;
    private final String channelId;
    private final String playlistURL;
    private final int volume;

    private AudioPlayer audioPlayer;
    private DefaultAudioPlayerManager playerManager;

    public RadioInstance(Logger logger, String homeGuildId, DiscordInstance discordInstance, String channelId, String playlistURL, int volume) {
        this.logger = logger;

        this.homeGuildId = homeGuildId;
        this.discordInstance = discordInstance;
        this.channelId = channelId;
        this.playlistURL = playlistURL;
        this.volume = volume;

        try {
            this.jda = JDABuilder.createDefault(discordInstance.getToken()).build();
        } catch (LoginException exception) {
            throw new IllegalArgumentException("Could not create JDA instance", exception);
        }
    }

    public void enable(DefaultAudioPlayerManager playerManager) {
        this.playerManager = playerManager;

        try {
            getJda().awaitReady();
            Guild guild = getJda().getGuildById(homeGuildId);

            if (guild == null) {
                logger.error("Radio " + getDiscordInstance().getName()
                        + " is not in guild " + homeGuildId);
                return;
            }

            VoiceChannel channel = guild.getVoiceChannelById(getChannelId());

            if (channel == null) {
                logger.error("Radio " + getDiscordInstance().getName()
                        + " could not find channel " + getChannelId());
                return;
            }

            guild.getAudioManager().openAudioConnection(channel);
            guild.getAudioManager().setAutoReconnect(true);

            audioPlayer = new DefaultAudioPlayer(playerManager);
            audioPlayer.setVolume(getVolume());

            guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(audioPlayer));

            loadTrack(getPlaylistURL()).thenAccept(tracks -> {
                if (tracks != null) {
                    QueueListener queueListener = new QueueListener(this, new ArrayDeque<>(tracks));
                    audioPlayer.addListener(queueListener);
                    audioPlayer.playTrack(queueListener.getTrackQueue().poll());

                    if (channel.getMembers().stream().allMatch(member -> member.getUser().isBot())) {
                        audioPlayer.setPaused(true);
                        logger.info("Paused radio {} on load because nobody was in channel {}",
                                getDiscordInstance().getName(), channel);
                    }
                }
            });
        } catch (InterruptedException exception) {
            logger.error("Interrupted while waiting for JDA load on instance "
                    + getDiscordInstance().getName(), exception);
            enable(playerManager);
        }
    }

    public void disable() {
        try {
            Objects.requireNonNull(getJda().getGuildById(homeGuildId)).getAudioManager().closeAudioConnection();
        } catch (NullPointerException ignored) {}

        audioPlayer.stopTrack();
        audioPlayer.destroy();
    }

    public void restart() {
        // we do not call disable() here, because that could cause some issues when re-opening the audio connection
        audioPlayer.stopTrack();
        audioPlayer.destroy();
        enable(playerManager);
    }

    public CompletableFuture<List<AudioTrack>> loadTrack(String query) {
        CompletableFuture<List<AudioTrack>> future = new CompletableFuture<>();

        playerManager.loadItem(query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                logger.info(getDiscordInstance().getName() + " - Loading Track " + query);
                future.complete(List.of(track));
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                logger.info(getDiscordInstance().getName() + " - Loading Playlist " + query);
                future.complete(playlist.getTracks());
            }

            @Override
            public void noMatches() {
                logger.error("No matches for playlist " + query);
                future.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                logger.error("Failed to load playlist " + query, exception);
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

    public DiscordInstance getDiscordInstance() {
        return discordInstance;
    }

    public JDA getJda() {
        return jda;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getPlaylistURL() {
        return playlistURL;
    }

    public int getVolume() {
        return volume;
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public DefaultAudioPlayerManager getPlayerManager() {
        return playerManager;
    }
}
