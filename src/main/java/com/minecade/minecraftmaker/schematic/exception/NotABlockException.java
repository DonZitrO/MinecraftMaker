package com.minecade.minecraftmaker.schematic.exception;

/**
 * Raised when an item is used when a block was expected.
 */
public class NotABlockException extends MinecraftMakerException {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new instance.
	 */
	public NotABlockException() {
		super("This item is not a block.");
	}

	/**
	 * Create a new instance.
	 *
	 * @param input
	 *            the input that was used
	 */
	public NotABlockException(String input) {
		super("The item '" + input + "' is not a block.");
	}

	/**
	 * Create a new instance.
	 *
	 * @param input
	 *            the input that was used
	 */
	public NotABlockException(int input) {
		super("The item with the ID " + input + " is not a block.");
	}

}
