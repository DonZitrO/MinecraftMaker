package com.minecade.minecraftmaker.schematic.exception;

/**
 * Thrown when there is an exception related to data handling.
 */
public class DataException extends Exception {

	private static final long serialVersionUID = 1L;

	public DataException(String msg) {
		super(msg);
	}

	public DataException(Throwable cause) {
		super(cause);
	}

	public DataException() {
		super();
	}

}
