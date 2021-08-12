package de.foxat.mercury.api.audio;

public interface GuildAudioManager {

    /**
     * Acquires or gets an instance to be connected to a discord channel
     *
     * @param guildId target guild id
     * @param channelId target channel id
     * @return Discord application id of the acquired instance
     * @throws IndexOutOfBoundsException there are no more instances available
     */
    String getOrAcquireInstanceId(String guildId, String channelId) throws IndexOutOfBoundsException;

    /**
     * Releases a discord instance to be connected to another channel
     *
     * @param guildId target guild id
     * @param channelId target channel id
     * @return Discord application id of released instance
     * @throws IllegalArgumentException there was no instance to release
     */
    String release(String guildId, String channelId) throws IllegalArgumentException;

}
