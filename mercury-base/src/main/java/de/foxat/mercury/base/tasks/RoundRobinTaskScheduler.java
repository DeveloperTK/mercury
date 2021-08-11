package de.foxat.mercury.base.tasks;

import de.foxat.mercury.api.Mercury;
import de.foxat.mercury.api.tasks.TaskScheduler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

public class RoundRobinTaskScheduler implements TaskScheduler, AutoCloseable {

    private static final Logger logger = LogManager.getLogger(RoundRobinTaskScheduler.class.getSimpleName());

    private final ScheduledExecutorService executorService;

    private final String uid;

    private final Mercury mercury;
    private final JDA[] instances;
    private final AtomicInteger counter;

    private final AtomicBoolean isClosed;

    public RoundRobinTaskScheduler(String guildId, Mercury mercury) {
        this.mercury = mercury;
        this.isClosed = new AtomicBoolean(false);
        this.uid = UUID.randomUUID().toString().substring(0, 6);

        List<JDA> availableInstances = new ArrayList<>();

        // TODO: WARNING - Root Instance is included in scheduled tasks
        for (JDA instance : mercury.getInstances()) {
            for (Guild guild : instance.getGuilds()) {
                if (guild.getId().equalsIgnoreCase(guildId)) {
                    availableInstances.add(instance);
                    break;
                }
            }
        }

        executorService = Executors.newSingleThreadScheduledExecutor();
        instances = availableInstances.toArray(new JDA[0]);
        counter = new AtomicInteger(0);

        logger.info("[{}] CREATED new TaskScheduler with {} available instances.", uid, instances.length);
    }

    private synchronized int getAndIncrement() {
        if (counter.get() >= instances.length) {
            counter.set(0);
        }

        return counter.getAndIncrement();
    }

    @Override
    public void submit(BiConsumer<JDA, Mercury> task) {
        if (isClosed.get()) throw new IllegalStateException("Task cannot be executed because the scheduler is closed");

        int id = getAndIncrement();
        logger.info("[{}] Submitted a task on instance #{}", uid, id);
        executorService.submit(() -> task.accept(instances[id], mercury));
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit timeUnit) throws InterruptedException {
        return executorService.awaitTermination(timeout, timeUnit);
    }

    @Override
    public void close() {
        logger.info("[{}] CLOSED task scheduler", uid);
        this.isClosed.set(true);
    }
}
