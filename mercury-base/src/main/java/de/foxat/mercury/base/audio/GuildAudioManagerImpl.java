package de.foxat.mercury.base.audio;

import de.foxat.mercury.api.audio.GuildAudioManager;
import de.foxat.mercury.base.discord.MercurySystem;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GuildAudioManagerImpl implements GuildAudioManager {

    private final Map<String, Map<String, String>> activeInstancesMap; // <guildId, <channelId, instanceId>>
    private final Map<String, Set<String>> availableInstancesMap;      // <guildId, {instanceId...}>

    private final MercurySystem mercurySystem;

    public GuildAudioManagerImpl(MercurySystem mercurySystem) {
        this.mercurySystem = mercurySystem;
        this.activeInstancesMap = new ConcurrentHashMap<>();
        this.availableInstancesMap = new ConcurrentHashMap<>();

        for (JDA instance : mercurySystem.getInstances().values()) {
            for (Guild guild : instance.getGuilds()) {
                if (availableInstancesMap.containsKey(guild.getId())) {
                    availableInstancesMap.get(guild.getId()).add(instance.getSelfUser().getId());
                } else {
                    Set<String> list = new HashSet<>();
                    list.add(instance.getSelfUser().getId());
                    availableInstancesMap.put(guild.getId(), list);
                }
            }
        }
    }

    @Override
    public synchronized String getOrAcquireInstanceId(String guildId, String channelId) throws IndexOutOfBoundsException {
        if (!activeInstancesMap.containsKey(guildId)) {
            activeInstancesMap.put(guildId, new HashMap<>());
        }

        if (activeInstancesMap.get(guildId).containsKey(channelId)) {
            return activeInstancesMap.get(guildId).get(channelId);
        } else {
            Set<String> availableInstanceSet = availableInstancesMap.get(guildId);
            Map<String, String> guildMap = activeInstancesMap.get(guildId);

            for (String instanceId : availableInstanceSet) {
                if (!guildMap.containsValue(instanceId)) {
                    guildMap.put(channelId, instanceId);
                    return instanceId;
                }
            }

            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public synchronized String release(String guildId, String channelId) throws IllegalArgumentException {
        if (!activeInstancesMap.containsKey(guildId)) {
            activeInstancesMap.put(guildId, new HashMap<>());
            throw new IllegalArgumentException();
        } else {
            Map<String, String> guildMap = activeInstancesMap.get(guildId);
            if (guildMap.containsKey(channelId)) {
                return guildMap.remove(channelId);
            } else {
                throw new IllegalArgumentException();
            }
        }
    }
}
