package com.minecade.minecraftmaker.function;

import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Performs a function on points in a region.
 */
public interface RegionFunction {

	/**
	 * Apply the function to the given position.
	 *
	 * @param position
	 *            the position
	 * @return true if something was changed
	 * @throws WorldEditException
	 *             thrown on an error
	 */
	public boolean apply(Vector position) throws MinecraftMakerException;

}
