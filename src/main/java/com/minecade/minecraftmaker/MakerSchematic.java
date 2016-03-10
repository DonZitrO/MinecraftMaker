package com.minecade.minecraftmaker;

public interface MakerSchematic {
	
	/**
	 * Pastes this schematic into the world.
	 * @return
	 */
	public boolean pasteSchematic(SlotBoundaries slot);

}
