package com.minecade.minecraftmaker.function.block;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.function.RegionFunction;
import com.minecade.minecraftmaker.function.pattern.Pattern;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Replaces blocks with a given pattern.
 */
public class BlockReplace implements RegionFunction {

	private final Extent extent;
	private Pattern pattern;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            an extent
	 * @param pattern
	 *            a pattern
	 */
	public BlockReplace(Extent extent, Pattern pattern) {
		checkNotNull(extent);
		checkNotNull(pattern);
		this.extent = extent;
		this.pattern = pattern;
	}

	@Override
	public boolean apply(Vector position) throws MinecraftMakerException {
		return extent.setBlock(position, pattern.apply(position));
	}

}