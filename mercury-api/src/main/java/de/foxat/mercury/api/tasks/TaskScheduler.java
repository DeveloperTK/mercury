package de.foxat.mercury.api.tasks;

import de.foxat.mercury.api.Mercury;
import net.dv8tion.jda.api.JDA;

import java.util.function.BiConsumer;

public interface TaskScheduler {

    /**
     * Submits a task which will be executed on one of the available instances
     * @param task task to be executed
     */
    void submit(BiConsumer<JDA, Mercury> task);

}
