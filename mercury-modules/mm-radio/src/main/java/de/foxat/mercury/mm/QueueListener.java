package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Queue;

public class QueueListener extends AudioEventAdapter {

    private static final Logger LOGGER = LogManager.getLogger(QueueListener.class.getSimpleName());

    private final RadioInstance parent;
    private final Queue<AudioTrack> trackQueue;

    public QueueListener(RadioInstance parent, Queue<AudioTrack> trackQueue) {
        this.parent = parent;
        this.trackQueue = trackQueue;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (trackQueue.isEmpty()) {
            parent.restart();
            LOGGER.info(parent.getDiscordInstance().getName() + " - Finished playing all tracks, restarting");
        } else {
            // makeClone is called to avoid problems with temporary resources
            player.playTrack(trackQueue.poll().makeClone());
            LOGGER.info(parent.getDiscordInstance().getName() + " - playing next track");
        }
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

    public Queue<AudioTrack> getTrackQueue() {
        return trackQueue;
    }
}
