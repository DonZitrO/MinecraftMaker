package com.minecade.minecraftmaker.function.mask;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * A mask that returns true whenever the block at the location is not an air
 * block (it contains some other block).
 */
public class ExistingBlockMask extends AbstractExtentMask {

	/**
	 * Create a new existing block map.
	 *
	 * @param extent
	 *            the extent to check
	 */
	public ExistingBlockMask(Extent extent) {
		super(extent);
	}

	@Override
	public boolean test(Vector vector) {
		return getExtent().getLazyBlock(vector).getType() != BlockID.AIR;
	}

	@Nullable
	@Override
	public Mask2D toMask2D() {
		return null;
	}

}
