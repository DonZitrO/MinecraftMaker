package com.minecade.minecraftmaker.schematic.function.operation;

import java.util.List;

import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

/**
 * An task that may be split into multiple steps to be run sequentially
 * immediately or at a varying or fixed interval. Operations should attempt
 * to break apart tasks into smaller tasks that can be completed in quicker
 * successions.
 */
public interface Operation {

    /**
     * Complete the next step. If this method returns true, then the method may
     * be called again in the future, or possibly never. If this method
     * returns false, then this method should not be called again.
     *
     * @param run describes information about the current run
     * @return another operation to run that operation again, or null to stop
     * @throws MinecraftMakerException an error
     */
    Operation resume(RunContext run) throws MinecraftMakerException;

    /**
     * Abort the current task. After the this method is called,
     * {@link #resume(RunContext)} should not be called at any point in the
     * future. This method should not be called after successful completion of
     * the operation. This method must be called if the operation is
     * interrupted before completion.
     */
    void cancel();

    /**
     * Add messages to the provided list that describe the current status
     * of the operation.
     *
     * @param messages The list to add messages to
     */
    void addStatusMessages(List<String> messages);

}
