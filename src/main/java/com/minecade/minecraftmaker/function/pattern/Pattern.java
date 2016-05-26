package com.minecade.minecraftmaker.function.pattern;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Returns a {@link BaseBlock} for a given position.
 */
public interface Pattern {

	/**
	 * Return a {@link BaseBlock} for the given position.
	 *
	 * @param position
	 *            the position
	 * @return a block
	 */
	BaseBlock apply(Vector position);

}
