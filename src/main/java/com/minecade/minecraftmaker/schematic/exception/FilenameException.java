package com.minecade.minecraftmaker.schematic.exception;

public class FilenameException extends MinecraftMakerException {

	private static final long serialVersionUID = 1L;

	private String filename;

	public FilenameException(String filename) {
		super();
		this.filename = filename;
	}

	public FilenameException(String filename, String msg) {
		super(msg);
		this.filename = filename;
	}

	public String getFilename() {
		return filename;
	}

}
