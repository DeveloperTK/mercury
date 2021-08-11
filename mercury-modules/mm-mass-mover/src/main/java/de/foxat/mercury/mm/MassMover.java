package de.foxat.mercury.mm;

import de.foxat.mercury.api.MercuryModule;
import de.foxat.mercury.api.tasks.TaskScheduler;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MassMover extends MercuryModule {

    private static final long moveTimeout = 25;
    private static final TimeUnit moveTimeoutUnit = TimeUnit.SECONDS;

    @Override
    protected void onLoad() {
        getMercury().getCommandRegistry().registerCommand(this,
                new CommandData("massmove", "Moves multiple people at once").addSubcommands(
                        new SubcommandData("direct", "Moves people from one channel into another").addOptions(
                                new OptionData(OptionType.CHANNEL, "from", "Source Channel", true),
                                new OptionData(OptionType.CHANNEL, "to", "Destination Channel", true)
                        ),
                        new SubcommandData("distribute", "Moves people from one channel").addOptions(
                                new OptionData(OptionType.CHANNEL, "from", "Source Channel", true),
                                new OptionData(OptionType.CHANNEL, "to", "Destination Category", true),
                                new OptionData(OptionType.INTEGER, "limit", "Fill channels with a " +
                                        "specified amount of clients, rather than distributing them equally", false)
                        ),
                        new SubcommandData("gather", "Moves all people into one channel (Use with caution!)").addOptions(
                                new OptionData(OptionType.CHANNEL, "to", "Destination Channel", true)
                        )
                )
        , false);
    }

    @Override
    protected void onEnable() {
        getMercury().getInstances().forEach(jda -> jda
                .getGuildById("847571600014442596")
                .getAudioManager()
                .openAudioConnection(jda.getVoiceChannelById("869634242584391770"))
        );
    }

    @Override
    protected void onDisable() {

    }

    @Override
    protected void onCommand(SlashCommandEvent slashCommandEvent) {
        if (slashCommandEvent.getSubcommandName() == null) {
            slashCommandEvent.reply("Invalid usage, please try again!").queue();
            return;
        }

        try {
            switch (slashCommandEvent.getSubcommandName()) {
                case "direct":
                    direct(slashCommandEvent);
                    return;
                case "distribute":
                    distribute(slashCommandEvent);
                    return;
                case "gather":
                    gather(slashCommandEvent);
                    return;
                default:
                    slashCommandEvent.reply("Invalid usage, please try again!").queue();
            }
        } catch (ClassCastException exception) {
            slashCommandEvent.reply("Internal type mismatch. Please check your inputs and try again!").queue();
        }
    }

    private void direct(SlashCommandEvent event) {
        VoiceChannel from = (VoiceChannel) Objects.requireNonNull(event.getOption("from")).getAsGuildChannel();
        VoiceChannel to = (VoiceChannel) Objects.requireNonNull(event.getOption("to")).getAsGuildChannel();

        InteractionHook interactionHook = event.deferReply(true).complete();
        TaskScheduler scheduler = getMercury().newRoundRobinScheduler(event.getGuild().getId());

        for (Member member : from.getMembers()) {
            scheduler.submit((jda, mercury) -> {
                Guild guild = jda.getGuildById(event.getGuild().getId());
                guild.moveVoiceMember(guild.getMember(member.getUser()), guild.getVoiceChannelById(to.getId())).queue();
            });
        }

        scheduler.shutdown();

        try {
            if (scheduler.awaitTermination(moveTimeout, moveTimeoutUnit)) {
                interactionHook.sendMessage("Moved members successfully").queue();
            } else {
                interactionHook.sendMessage("Could not move all members in time. Maybe try more bots or fewer people").queue();
            }
        } catch (InterruptedException e) {
            interactionHook.sendMessage("Interrupted while awaiting completion of tasks").queue();
        }
    }

    private void distribute(SlashCommandEvent event) {
        VoiceChannel from = (VoiceChannel) Objects.requireNonNull(event.getOption("from")).getAsGuildChannel();
        Category to = (Category) Objects.requireNonNull(event.getOption("to")).getAsGuildChannel();
        OptionMapping limitOption = event.getOption("limit");

        InteractionHook interactionHook = event.deferReply(true).complete();
        TaskScheduler scheduler = getMercury().newRoundRobinScheduler(event.getGuild().getId());

        if (limitOption != null && limitOption.getAsLong() > 0) {
            if (from.getMembers().size() > to.getVoiceChannels().size() * limitOption.getAsLong()) {
                interactionHook.sendMessage(
                        String.format("Cannot fit %s people in %s channels with a limit of %s per channel (minimum needed: %s)",
                                from.getMembers().size(),
                                to.getVoiceChannels().size(),
                                limitOption.getAsLong(),
                                (int) Math.ceil((float) from.getMembers().size() / (float) to.getVoiceChannels().size())
                        )
                ).queue();
                return;
            } else {
                distributeLimited(from, to, limitOption.getAsLong(), scheduler, event);
            }
        } else {
            distributeEqually(from, to, scheduler, event);
        }

        scheduler.shutdown();

        try {
            if (scheduler.awaitTermination(moveTimeout, moveTimeoutUnit)) {
                interactionHook.sendMessage("Moved members successfully").queue();
            } else {
                interactionHook.sendMessage("Could not move all members in time. Maybe try more bots or fewer people").queue();
            }
        } catch (InterruptedException e) {
            interactionHook.sendMessage("Interrupted while awaiting completion of tasks").queue();
        }
    }

    private void distributeEqually(VoiceChannel from, Category to, TaskScheduler scheduler, SlashCommandEvent event) {
        List<VoiceChannel> voiceChannels = to.getVoiceChannels();
        int channelCount = voiceChannels.size();
        int currentChannel = 0;

        for (Member member : from.getMembers()) {
            currentChannel++;
            if (currentChannel >= channelCount) currentChannel = 0;

            final int finalCurrentChannel = currentChannel; // why java...
            scheduler.submit((jda, mercury) -> {
                Guild guild = jda.getGuildById(event.getGuild().getId());
                guild.moveVoiceMember(
                        guild.getMember(member.getUser()),
                        guild.getVoiceChannelById(voiceChannels.get(finalCurrentChannel).getId())
                ).queue();
            });
        }
    }

    private void distributeLimited(VoiceChannel from, Category to, long limit, TaskScheduler scheduler, SlashCommandEvent event) {
        Iterator<VoiceChannel> voiceChannelIterator = to.getVoiceChannels().iterator();
        VoiceChannel currentChannel = voiceChannelIterator.next();
        int counter = -1;

        for (Member member : from.getMembers()) {
            counter ++;
            if (counter >= limit) {
                counter = 0;
                currentChannel = voiceChannelIterator.next();
            }

            final VoiceChannel finalCurrentChannel = currentChannel; // why java...
            scheduler.submit((jda, mercury) -> {
                Guild guild = jda.getGuildById(event.getGuild().getId());
                guild.moveVoiceMember(
                        guild.getMember(member.getUser()),
                        guild.getVoiceChannelById(finalCurrentChannel.getId())
                ).queue();
            });
        }
    }

    private void gather(SlashCommandEvent event) {
        VoiceChannel to = (VoiceChannel) Objects.requireNonNull(event.getOption("to")).getAsGuildChannel();
        InteractionHook interactionHook = event.deferReply(true).complete();

        List<String> members = new ArrayList<>();

        for (VoiceChannel voiceChannel : event.getGuild().getVoiceChannels()) {
            for (Member member : voiceChannel.getMembers()) {
                members.add(member.getId());
            }
        }

        TaskScheduler scheduler = getMercury().newRoundRobinScheduler(event.getGuild().getId());

        for (String memberId : members) {
            scheduler.submit((jda, mercury) -> {
                Guild guild = jda.getGuildById(event.getGuild().getId());
                guild.moveVoiceMember(guild.getMemberById(memberId), guild.getVoiceChannelById(to.getId())).queue();
            });
        }

        scheduler.shutdown();

        try {
            if (scheduler.awaitTermination(moveTimeout, moveTimeoutUnit)) {
                interactionHook.sendMessage("Moved members successfully").queue();
            } else {
                interactionHook.sendMessage("Could not move all members in time. Maybe try more bots or fewer people").queue();
            }
        } catch (InterruptedException e) {
            interactionHook.sendMessage("Interrupted while awaiting completion of tasks").queue();
        }
    }
}
