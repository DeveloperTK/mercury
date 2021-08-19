package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.*;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import de.foxat.mercury.api.MercuryModule;
import de.foxat.mercury.api.audio.AudioPlayerSendHandler;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MercuryRadio extends MercuryModule {

    private XMLRadioConfiguration radioConfig;
    private DefaultAudioPlayerManager playerManager;

    private JoinLeaveListener joinLeaveListener;
    private String listeningInstanceName;

    @Override
    protected void onLoad() {
        getModuleDirectory().mkdirs();

        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);

        try {
            radioConfig = XMLRadioConfiguration.create(
                    new File(getModuleDirectory().getPath() + "/radio.xml"),
                    getLogger()
            );
        } catch (IllegalStateException exception) {
            Error error = new ExceptionInInitializerError(exception);
            error.initCause(exception);
            getLogger().error(error);
            throw error;
        }

        joinLeaveListener = new JoinLeaveListener(radioConfig.getRadios().values());
    }

    @Override
    protected void onEnable() {
        radioConfig.getRadios().values().forEach(this::enable);

        for (RadioInstance radio : radioConfig.getRadios().values()) {
            Guild home = radio.getJda().getGuildById(radioConfig.getHomeGuildId());
            if (home != null) {
                listeningInstanceName = radio.getDiscordInstance().getName();
                radio.getJda().addEventListener(joinLeaveListener);
                getLogger().info("Added JoinLeaveListener on instance named " + listeningInstanceName);
                break;
            }
        }
    }

    @Override
    protected void onDisable() {
        radioConfig.getRadios().values().forEach(this::disable);

        if (listeningInstanceName != null) {
            radioConfig.getRadios().get(listeningInstanceName).getJda().removeEventListener(joinLeaveListener);
            getLogger().info("Removed JoinLeaveListener on instance named " + listeningInstanceName);
        } else {
            getLogger().info("Did not remove any JoinLeaveListeners, none were registered.");
        }
    }

    private void enable(RadioInstance radio) {
        try {
            radio.getJda().awaitReady();
            Guild guild = radio.getJda().getGuildById(radioConfig.getHomeGuildId());

            if (guild == null) {
                getLogger().error("Radio " + radio.getDiscordInstance().getName()
                        + " is not in guild " + radioConfig.getHomeGuildId());
                return;
            }

            VoiceChannel channel = guild.getVoiceChannelById(radio.getChannelId());

            if (channel == null) {
                getLogger().error("Radio " + radio.getDiscordInstance().getName()
                        + " could not find channel " + radio.getChannelId());
                return;
            }

            guild.getAudioManager().openAudioConnection(channel);
            guild.getAudioManager().setAutoReconnect(true);

            AudioPlayer player = new DefaultAudioPlayer(playerManager);
            player.addListener(new AudioRepeatListener(this));
            player.setVolume(radio.getVolume());
            radio.setAudioPlayer(player);

            guild.getAudioManager().setSendingHandler(new AudioPlayerSendHandler(player));

            loadTrack(radio.getPlaylistURL()).thenAccept(track -> {
                if (track != null) {
                    player.playTrack(track);

                    if (channel.getMembers().stream().allMatch(member -> member.getUser().isBot())) {
                        player.setPaused(true);
                        getLogger().info("Paused radio {} on load because nobody was in channel {}",
                                radio.getDiscordInstance().getName(), channel);
                    }
                }
            });
        } catch (InterruptedException exception) {
            getLogger().error("Interrupted while waiting for JDA load on instance "
                    + radio.getDiscordInstance().getName(), exception);
        }
    }

    private void disable(RadioInstance instance) {
        try {
            Objects.requireNonNull(instance.getJda().getGuildById(radioConfig.getHomeGuildId()))
                    .getAudioManager().closeAudioConnection();
        } catch (NullPointerException ignored) {}

        try {
            instance.getAudioPlayer().stopTrack();
        } catch (NullPointerException ignored) {}

        instance.getJda().shutdown();
    }

    public CompletableFuture<AudioTrack> loadTrack(String query) {
        CompletableFuture<AudioTrack> future = new CompletableFuture<>();

        playerManager.loadItem(query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                future.complete(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                getLogger().warn("Only playing first track of playlist " + query);
                future.complete(playlist.getTracks().get(0));
            }

            @Override
            public void noMatches() {
                getLogger().error("No matches for playlist " + query);
                future.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                getLogger().error("Failed to load playlist " + query, exception);
                future.completeExceptionally(exception);
            }
        });

        return future;
    }

}
