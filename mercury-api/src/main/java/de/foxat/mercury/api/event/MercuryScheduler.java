package de.foxat.mercury.api.event;

import java.time.LocalDateTime;
import java.util.concurrent.*;

public interface MercuryScheduler {

    /**
     * Submit a Runnable which will be executed as fast as possible
     * while not blocking any internal procedures and ensuring the
     * task can get monitor access on internal objects
     *
     * @param task task to be executed
     * @see ExecutorService#submit(Runnable)
     * @throws RejectedExecutionException thrown by the executor if
     *                    the task cannot be executed at the moment
     */
    Future<?> submit(Runnable task);

    /**
     * Executes a runnable once with a fixed initial delay
     * @param delay the time from now to delay execution
     * @param timeUnit the time unit of the delay parameter
     * @param task the task to execute
     * @see ScheduledExecutorService#schedule(Runnable, long, TimeUnit)
     * @throws RejectedExecutionException thrown by the executor if
     *                    the task cannot be executed at the moment
     */
    ScheduledFuture<?> submitWithFixedDelay(long delay, TimeUnit timeUnit, Runnable task);

    /**
     * Schedules a runnable to be executed at a certain time.
     *
     * @param date execution date in UTC time
     * @param task task to be executed
     * @see ScheduledExecutorService#scheduleWithFixedDelay(Runnable, long, long, TimeUnit)
     * @throws RejectedExecutionException thrown by the executor if
     *                    the task cannot be executed at the moment
     */
    ScheduledFuture<?> submitWithTargetTime(LocalDateTime date, Runnable task);

    /**
     * Schedules a runnable to be executed at a fixed rate
     * with an optional initial delay.
     *
     * @param initialDelay the time to delay first execution
     * @param interval the period between successive executions
     * @param timeUnit the time unit of the initialDelay and period parameters
     * @param task the task to execute
     * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
     */
    ScheduledFuture<?> scheduleAtFixedRate(long initialDelay, long interval, TimeUnit timeUnit, Runnable task);

    /**
     * Schedules a runnable to be executed at a fixed rate
     *
     * @param interval the period between successive executions
     * @param timeUnit the time unit of the initialDelay and period parameters
     * @param task the task to execute
     * @see ScheduledExecutorService#scheduleAtFixedRate(Runnable, long, long, TimeUnit)
     */
    ScheduledFuture<?> scheduleAtFixedRate(long interval, TimeUnit timeUnit, Runnable task);

}
