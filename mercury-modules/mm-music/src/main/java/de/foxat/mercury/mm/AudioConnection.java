package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayDeque;
import java.util.Queue;

public class AudioConnection extends AudioEventAdapter {

    private static final Logger LOGGER = LogManager.getLogger(AudioConnection.class.getName());

    private final Queue<AudioTrack> trackQueue;

    private final AudioPlayer audioPlayer;

    public AudioConnection(AudioPlayer audioPlayer) {
        trackQueue = new ArrayDeque<>();
        this.audioPlayer = audioPlayer;
        audioPlayer.addListener(this);
    }

    public AudioPlayer getAudioPlayer() {
        return audioPlayer;
    }

    public Queue<AudioTrack> getTrackQueue() {
        return trackQueue;
    }

    /**
     * Enqueues or plays a track
     *
     * @param track The track to be enqueued
     * @return true - the track was enqueued, false - the track was played immediately
     */
    public boolean enqueueTrack(AudioTrack track) {
        synchronized (trackQueue) {
            // if no track is currently playing
            if (audioPlayer.getPlayingTrack() == null
                    || audioPlayer.getPlayingTrack().getState().equals(AudioTrackState.INACTIVE)
                    || audioPlayer.getPlayingTrack().getState().equals(AudioTrackState.FINISHED)) {
                audioPlayer.playTrack(track);
                return false;
            } else {
                trackQueue.add(track);
                return true;
            }
        }
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        LOGGER.info("Track stopped. mayStartNext: {}", endReason.mayStartNext);
        if (endReason.mayStartNext) {
            // Start next track
            player.playTrack(trackQueue.poll());
        }

        // endReason == FINISHED: A track finished or died by an exception (mayStartNext = true).
        // endReason == LOAD_FAILED: Loading of a track failed (mayStartNext = true).
        // endReason == STOPPED: The player was stopped.
        // endReason == REPLACED: Another track started playing while this had not finished
        // endReason == CLEANUP: Player hasn't been queried for a while, if you want you can put a
        //                       clone of this back to your queue
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        // An already playing track threw an exception (track end event will still be received separately)
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        // Audio track has been unable to provide us any audio, might want to just start a new track

    }
}
