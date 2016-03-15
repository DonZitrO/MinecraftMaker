package com.minecade.minecraftmaker.schematic.exception;

/**
 * Parent for all MCMaker exceptions.
 */
public abstract class MinecraftMakerException extends Exception {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new exception.
	 */
	protected MinecraftMakerException() {
	}

	/**
	 * Create a new exception with a message.
	 *
	 * @param message
	 *            the message
	 */
	protected MinecraftMakerException(String message) {
		super(message);
	}

	/**
	 * Create a new exception with a message and a cause.
	 *
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	protected MinecraftMakerException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Create a new exception with a cause.
	 *
	 * @param cause
	 *            the cause
	 */
	protected MinecraftMakerException(Throwable cause) {
		super(cause);
	}
}
