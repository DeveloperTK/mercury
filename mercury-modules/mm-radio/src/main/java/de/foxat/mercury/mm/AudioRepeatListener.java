package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AudioRepeatListener extends AudioEventAdapter {

    private static final Logger LOGGER = LogManager.getLogger(AudioRepeatListener.class.getSimpleName());

    private final RadioInstance parent;

    public AudioRepeatListener(RadioInstance parent) {
        this.parent = parent;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        player.playTrack(track.makeClone());
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        LOGGER.error("Audio track threw exception, restarting", exception);
        parent.restart();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        LOGGER.error("Audio track was stuck, restarting");
        parent.restart();
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs, StackTraceElement[] stackTrace) {
        LOGGER.error("Audio track was stuck, restarting");
        parent.restart();
    }
}
