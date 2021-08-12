package de.foxat.mercury.base.discord;

import de.foxat.mercury.api.config.DiscordInstance;
import de.foxat.mercury.base.config.XMLMercuryConfig;
import de.foxat.mercury.base.util.MercuryUtils;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MercurySystem {

    private static final Logger logger = LogManager.getLogger(MercurySystem.class.getSimpleName());

    private final Map<String, JDA> instances;

    public MercurySystem() {
        instances = new HashMap<>();
    }

    public void startup() {
        XMLMercuryConfig.getInstance().getInstances().forEach(this::loadInstance);

        // ...

        instances.forEach(this::finalizeStartup);

        for (JDA instance : instances.values()) {
            try {
                instance.awaitReady();
            } catch (InterruptedException exception) {
                logger.error("Unexpected Thread Interrupt", exception);
            }
        }
    }

    private void loadInstance(DiscordInstance instance) {
        try {
            instances.put(instance.getId(), JDABuilder.createDefault(instance.getToken())
                    .setStatus(OnlineStatus.IDLE).setActivity(Activity.watching("startup...")).build());

            preventLooseWorkerInstances(instances.get(instance.getId()));

        } catch (LoginException exception) {
            logger.error("Caught LoginException at instance " + instance, exception);
        }
    }

    private void preventLooseWorkerInstances(JDA instance) {
        String rootId = XMLMercuryConfig.getInstance().getRootInstance().getId();

        if (instance.getSelfUser().getId().equals(rootId)) return;

        instance.addEventListener(new ListenerAdapter() {
            @Override
            public void onGuildJoin(@NotNull GuildJoinEvent event) {
                if (getInstances().get(rootId).getGuildById(event.getGuild().getId()) == null) {
                    logger.error("Tried inviting instance " + event.getJDA().getSelfUser().getId()
                            + " to a server missing the root instance, aborting...");
                    event.getGuild().getDefaultChannel().sendMessage("Cannot enter server without root instance. "
                            + "Please invite the root instance first before adding workers!").queue();
                    event.getGuild().leave().queue();
                }
            }
        });
    }

    private void finalizeStartup(String id, JDA jda) {
        MercuryUtils.invokeLater(() -> {
            try {
                jda.awaitReady();
                jda.getPresence().setActivity(null);
                jda.getPresence().setStatus(OnlineStatus.ONLINE);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        });
    }

    /**
     * @return all JDA instances, logged in or not
     */
    public Map<String, JDA> getInstances() {
        return instances;
    }

    /**
     * @return all JDA instances currently logged in
     */
    public Map<String, JDA> getActiveInstances() {
        return instances.entrySet().parallelStream()
                .filter(entry -> entry.getValue().getStatus().isInit())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (prev, next) -> next, HashMap::new));
    }
}
