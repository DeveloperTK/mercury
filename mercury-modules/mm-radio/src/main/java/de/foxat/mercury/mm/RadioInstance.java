package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import de.foxat.mercury.api.config.DiscordInstance;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;

public class RadioInstance {

    private final DiscordInstance discordInstance;
    private final JDA jda;
    private final String channelId;
    private final String playlistURL;
    private final int volume;

    private AudioPlayer audioPlayer;

    public RadioInstance(DiscordInstance discordInstance, String channelId, String playlistURL, int volume) {
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

    public void setAudioPlayer(AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
    }
}
