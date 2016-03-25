package com.minecade.minecraftmaker.schematic.exception;

public class FileSelectionAbortedException extends FilenameException {

	private static final long serialVersionUID = 1L;

	public FileSelectionAbortedException() {
		super("");
	}

	public FileSelectionAbortedException(String msg) {
		super("", msg);
	}

}
