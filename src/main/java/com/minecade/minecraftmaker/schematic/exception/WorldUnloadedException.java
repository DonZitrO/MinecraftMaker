package com.minecade.minecraftmaker.schematic.exception;

/**
 * Thrown if the world has been unloaded.
 */
public class WorldUnloadedException extends MinecraftMakerException {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new instance.
	 */
	public WorldUnloadedException() {
		super("The world was unloaded already");
	}
	
}
