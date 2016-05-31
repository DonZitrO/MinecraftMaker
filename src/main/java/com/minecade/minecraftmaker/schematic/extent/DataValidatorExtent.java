package com.minecade.minecraftmaker.schematic.extent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.World;

/**
 * Validates set data to prevent creating invalid blocks and such.
 */
public class DataValidatorExtent extends AbstractDelegateExtent {

	private final World world;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent
	 * @param world
	 *            the world
	 */
	public DataValidatorExtent(Extent extent, World world) {
		super(extent);
		checkNotNull(world);
		this.world = world;
	}

	@Override
	public boolean setBlock(Vector location, BaseBlock block) throws MinecraftMakerException {
		final int y = location.getBlockY();
		final int type = block.getType();
		if (y < 0 || y > world.getMaxY()) {
			return false;
		}

		// No invalid blocks
		if (!world.isValidBlockType(type)) {
			return false;
		}

		if (block.getData() < 0) {
			throw new SevereValidationException("Cannot set a data value that is less than 0");
		}

		return super.setBlock(location, block);
	}

	private static class SevereValidationException extends MinecraftMakerException {
		private static final long serialVersionUID = 1L;

		private SevereValidationException(String message) {
			super(message);
		}
	}

}
