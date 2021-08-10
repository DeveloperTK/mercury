package de.foxat.mercury.base.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.*;

public class MercuryUtils {

    private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final Logger logger = LogManager.getLogger(MercuryUtils.class.getSimpleName());

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                executorService.shutdown();
                logger.info("Shutting down executorService, awaiting termination.");
                executorService.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                logger.warn("executorService was shut down abruptly", exception);
            }
        }));
    }

    public static synchronized Future<?> invokeLater(Runnable runnable) {
        logger.info("MercuryUtils#invokeLater submitted a task");
        return executorService.submit(runnable);
    }

}
