package de.foxat.mercury.base.event;

import de.foxat.mercury.api.event.MercuryScheduler;
import de.foxat.mercury.base.util.MercuryUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.*;

public class MercurySchedulerImpl implements MercuryScheduler {

    ScheduledExecutorService executorService;

    public MercurySchedulerImpl() {
        this.executorService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public Future<?> submit(Runnable task) {
        return MercuryUtils.invokeLater(task);
    }

    @Override
    public ScheduledFuture<?> submitWithFixedDelay(long delay, TimeUnit timeUnit, Runnable task) {
        return executorService.schedule(task, delay, timeUnit);
    }

    @Override
    public ScheduledFuture<?> submitWithTargetTime(LocalDateTime date, Runnable task) {
        long secondDelay = date.toEpochSecond(ZoneOffset.UTC) - LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        return executorService.schedule(task, secondDelay, TimeUnit.SECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(long initialDelay, long interval, TimeUnit timeUnit, Runnable task) {
        return executorService.scheduleAtFixedRate(task, initialDelay, initialDelay, timeUnit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(long interval, TimeUnit timeUnit, Runnable task) {
        return executorService.scheduleAtFixedRate(task, 0L, interval, timeUnit);
    }
}
