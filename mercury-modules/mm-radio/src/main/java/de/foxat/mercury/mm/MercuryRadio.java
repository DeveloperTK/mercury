package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import de.foxat.mercury.api.MercuryModule;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.managers.AudioManager;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MercuryRadio extends MercuryModule {

    private XMLRadioConfiguration radioConfig;
    private DefaultAudioPlayerManager playerManager;

    private JoinLeaveListener joinLeaveListener;
    private String listeningInstanceName;

    private ScheduledExecutorService healthCheckExecutor;

    @Override
    protected void onLoad() {
        getModuleDirectory().mkdirs();

        playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);

        try {
            radioConfig = XMLRadioConfiguration.create(
                    new File(getModuleDirectory().getPath() + "/radio.xml"),
                    this
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
        for (RadioInstance radio : radioConfig.getRadios().values()) {
            radio.enable(playerManager);
            Guild home = radio.getJda().getGuildById(radioConfig.getHomeGuildId());
            if (home != null) {
                listeningInstanceName = radio.getDiscordInstance().getName();
                radio.getJda().addEventListener(joinLeaveListener);
                getLogger().info("Added JoinLeaveListener on instance named " + listeningInstanceName);
                break;
            }
        }

        healthCheckExecutor = Executors.newSingleThreadScheduledExecutor();
        healthCheckExecutor.scheduleAtFixedRate(this::checkActiveConnections, 1, 5, TimeUnit.MINUTES);
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

        try {
            healthCheckExecutor.shutdown();
            healthCheckExecutor.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {}
    }

    private void disable(RadioInstance instance) {
        instance.disable();
        instance.getJda().shutdown();
    }

    /**
     * Check if all discord bots are connected and restarts them if needed
     */
    private void checkActiveConnections() {
        final String guildId = radioConfig.getHomeGuildId();
        for (RadioInstance radio : radioConfig.getRadios().values()) {
            VoiceChannel target = Objects.requireNonNull(radio.getJda().getVoiceChannelById(radio.getChannelId()));
            AudioManager audioManager = Objects.requireNonNull(radio.getJda().getGuildById(guildId)).getAudioManager();
            VoiceChannel current = Objects.requireNonNull(audioManager.getConnectedChannel());

            if (!audioManager.isConnected() || !current.getId().equals(target.getId())) {
                getLogger().warn("Radio \"" + radio.getDiscordInstance().getName() + "\" was disconnected, restarting");
                radio.restart();
            } else if (radio.getAudioPlayer().isPaused()) {
                // unpause if paused and potentially trigger an error which would lead to a restart anyways
                getLogger().warn("Radio \"" + radio.getDiscordInstance().getName() + "\" was paused, resuming");
                radio.getAudioPlayer().setPaused(false);
            }
        }
    }

}
