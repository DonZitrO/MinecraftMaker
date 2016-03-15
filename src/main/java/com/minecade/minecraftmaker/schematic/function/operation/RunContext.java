package com.minecade.minecraftmaker.schematic.function.operation;

/**
 * Describes the current run.
 */
public class RunContext {

	/**
	 * Return whether the current operation should still continue running.
	 *
	 * <p>
	 * This method can be called frequently.
	 * </p>
	 *
	 * @return true if the operation should continue running
	 */
	public boolean shouldContinue() {
		return true;
	}

}
