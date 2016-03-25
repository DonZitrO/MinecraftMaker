package com.minecade.minecraftmaker.schematic.exception;

public class InvalidFilenameException extends FilenameException {

	private static final long serialVersionUID = 1L;

	public InvalidFilenameException(String filename) {
		super(filename);
	}

	public InvalidFilenameException(String filename, String msg) {
		super(filename, msg);
	}

}
