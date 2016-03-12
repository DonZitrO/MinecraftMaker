package com.minecade.minecraftmaker.exception;

import com.sk89q.worldedit.world.DataException;

public class ChunkStoreException extends DataException {

	private static final long serialVersionUID = 1L;

	public ChunkStoreException(String msg) {
		super(msg);
	}

	public ChunkStoreException() {
		super();
	}

}
