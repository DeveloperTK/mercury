package de.foxat.mercury.mm;

import de.foxat.mercury.api.config.DiscordInstance;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import okhttp3.*;
import okhttp3.internal.annotations.EverythingIsNonNull;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JoinLeaveListener extends ListenerAdapter {

    private static final Logger logger = LogManager.getLogger(JoinLeaveListener.class.getSimpleName());

    private final Collection<RadioInstance> radios;

    public JoinLeaveListener(Collection<RadioInstance> radios) {
        this.radios = radios;
    }

    @Override
    public void onGuildVoiceJoin(@NotNull GuildVoiceJoinEvent event) {
        List<RadioInstance> channelRadios = radios.stream()
                .filter(radio -> radio.getChannelId().equals(event.getChannelJoined().getId()))
                .collect(Collectors.toList());

        for (RadioInstance channelRadio : channelRadios) {
            if (event.getMember().getId().equals(channelRadio.getDiscordInstance().getId())) {
                unmuteSelf(event, channelRadio.getDiscordInstance());
            } else if (!allMembersBots(event.getChannelJoined())) {
                logger.info("Resumed radio named " + channelRadio.getDiscordInstance().getName());
                channelRadio.getAudioPlayer().setPaused(false);
            }
        }
    }

    private void unmuteSelf(GuildVoiceJoinEvent event, DiscordInstance instance) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://discord.com/api/v9/guilds/" + event.getGuild().getId() + "/members/" + instance.getId())
                .patch(RequestBody.create(MediaType.get("application/json"), "{\"mute\": false}"))
                .header("Authorization", "Bot " + instance.getToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            @EverythingIsNonNull
            public void onFailure(Call call, IOException exception) {
                logger.error("API Call failed", exception);
            }

            @Override
            @EverythingIsNonNull
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() != 204) {
                    assert response.body() != null;
                    logger.info("Unmuted via API call (probably...)\n" + response.body().string());
                }
            }
        });
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        List<RadioInstance> leftChannelRadios = radios.stream()
                .filter(radio -> radio.getChannelId().equals(event.getChannelLeft().getId()))
                .collect(Collectors.toList());

        leftChannelRadios.stream()
                .filter(radio -> radio.getDiscordInstance().getId().equals(event.getMember().getId()))
                .forEach(RadioInstance::restart);

        if (event.getMember().getUser().isBot()) return;

        List<RadioInstance> joinedChannelRadios = radios.stream()
                .filter(radio -> radio.getChannelId().equals(event.getChannelJoined().getId()))
                .collect(Collectors.toList());

        for (RadioInstance channelRadio : joinedChannelRadios) {
            if (!allMembersBots(event.getChannelJoined())) {
                logger.info("Resumed radio named " + channelRadio.getDiscordInstance().getName());
                channelRadio.getAudioPlayer().setPaused(false);
            }
        }

        for (RadioInstance channelRadio : leftChannelRadios) {
            if (allMembersBots(event.getChannelLeft())) {
                logger.info("Paused radio named " + channelRadio.getDiscordInstance().getName());
                channelRadio.getAudioPlayer().setPaused(true);
            }
        }
    }

    @Override
    public void onGuildVoiceLeave(@NotNull GuildVoiceLeaveEvent event) {
        List<RadioInstance> channelRadios = radios.stream()
                .filter(radio -> radio.getChannelId().equals(event.getChannelLeft().getId()))
                .collect(Collectors.toList());

        channelRadios.stream()
                .filter(radio -> radio.getDiscordInstance().getId().equals(event.getMember().getId()))
                .forEach(RadioInstance::restart);

        if (event.getMember().getUser().isBot()) return;

        for (RadioInstance channelRadio : channelRadios) {
            if (allMembersBots(event.getChannelLeft())) {
                logger.info("Paused radio named " + channelRadio.getDiscordInstance().getName());
                channelRadio.getAudioPlayer().setPaused(true);
            }
        }
    }

    private boolean allMembersBots(VoiceChannel channel) {
        return channel.getMembers().stream().allMatch(member -> member.getUser().isBot());
    }

    private VoiceChannel getOtherThanAFKChannel(GuildVoiceJoinEvent event) {
        if (event.getGuild().getAfkChannel() == null) return event.getChannelJoined();

        for (VoiceChannel voiceChannel : event.getGuild().getVoiceChannels()) {
            if (!voiceChannel.getId().equals(event.getGuild().getAfkChannel().getId())) {
                return voiceChannel;
            }
        }

        return event.getChannelJoined();
    }

    @Override
    public void onGuildVoiceGuildMute(@NotNull GuildVoiceGuildMuteEvent event) {
        System.out.println(event.getMember() + " was " + (event.getVoiceState().isMuted() ? "" : "un") + "muted" );
    }
}
