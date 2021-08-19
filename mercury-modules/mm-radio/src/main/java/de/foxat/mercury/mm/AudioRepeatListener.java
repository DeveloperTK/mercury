package de.foxat.mercury.mm;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

public class AudioRepeatListener extends AudioEventAdapter {

    private final MercuryRadio parent;

    public AudioRepeatListener(MercuryRadio parent) {
        this.parent = parent;
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        player.playTrack(track);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        parent.getLogger().error("Audio track threw exception", exception);
        player.stopTrack();
        parent.loadTrack(track.getInfo().uri).thenAccept(player::playTrack);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        parent.getLogger().error("Audio track was stuck, restarting");
        player.stopTrack();
        parent.loadTrack(track.getInfo().uri).thenAccept(player::playTrack);
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs, StackTraceElement[] stackTrace) {
        parent.getLogger().error("Audio track was stuck, restarting");
        player.stopTrack();
        parent.loadTrack(track.getInfo().uri).thenAccept(player::playTrack);
    }
}