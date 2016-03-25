package com.minecade.minecraftmaker.schematic.exception;

public class FilenameResolutionException extends FilenameException {

	private static final long serialVersionUID = 1L;

	public FilenameResolutionException(String filename) {
		super(filename);
	}

	public FilenameResolutionException(String filename, String msg) {
		super(filename, msg);
	}

}
