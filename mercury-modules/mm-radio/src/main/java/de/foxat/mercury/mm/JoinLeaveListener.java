package de.foxat.mercury.mm;

import de.foxat.mercury.api.config.DiscordInstance;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import okhttp3.*;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class JoinLeaveListener extends ListenerAdapter {

    private static final Logger logger = LogManager.getLogger(JoinLeaveListener.class.getSimpleName());

    private final Collection<RadioInstance> radios;

    private boolean unmuting = false;

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
                // TODO: :woozy_face:
                jazz(event, channelRadio.getDiscordInstance());
                /*
                if (unmuting) return; else unmuting = true;
                // event.getGuild().moveVoiceMember(event.getMember(), getOtherThanAFKChannel(event)).queue();
                event.getMember().mute(true).queue();
                // event.getGuild().moveVoiceMember(event.getMember(), event.getChannelJoined()).queue();
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                event.getMember().mute(false).queue();
                unmuting = false;
                 */
                /*if (event.getMember().getVoiceState().isGuildMuted()) {
                    channelRadio.getJda().getGuildById(event.getGuild().getId()).getAudioManager().closeAudioConnection();
                    channelRadio.getJda().getGuildById(event.getGuild().getId()).getAudioManager().openAudioConnection(event.getChannelJoined());
                }*/
            } else if (!allMembersBots(event.getChannelJoined())) {
                logger.info("Resumed radio named " + channelRadio.getDiscordInstance().getName());
                channelRadio.getAudioPlayer().setPaused(false);
            }
        }
    }

    private void jazz(GuildVoiceJoinEvent event, DiscordInstance instance) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://discord.com/api/v9/guilds/" + event.getGuild().getId() + "/members/" + instance.getId())
                .patch(RequestBody.create(MediaType.get("application/json"), "{\"mute\": false}"))
                .header("Authorization", "Bot " + instance.getToken())
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                logger.error("API Call failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() != 204) logger.info("Unmuted via API (probably...)\n" + response.body().string());
            }
        });
    }

    @Override
    public void onGuildVoiceMove(@NotNull GuildVoiceMoveEvent event) {
        if (event.getMember().getUser().isBot()) return;

        List<RadioInstance> joinedChannelRadios = radios.stream()
                .filter(radio -> radio.getChannelId().equals(event.getChannelJoined().getId()))
                .collect(Collectors.toList());

        List<RadioInstance> leftChannelRadios = radios.stream()
                .filter(radio -> radio.getChannelId().equals(event.getChannelLeft().getId()))
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
        if (event.getMember().getUser().isBot()) return;

        List<RadioInstance> channelRadios = radios.stream()
                .filter(radio -> radio.getChannelId().equals(event.getChannelLeft().getId()))
                .collect(Collectors.toList());

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
