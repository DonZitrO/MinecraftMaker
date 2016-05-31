package com.minecade.minecraftmaker.schematic.world;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.extent.AbstractDelegateExtent;
import com.minecade.minecraftmaker.schematic.extent.Extent;

/**
 * Returns the same cached {@link BaseBlock} for repeated calls to
 * {@link #getLazyBlock(Vector)} with the same position.
 */
public class LastAccessExtentCache extends AbstractDelegateExtent {

	private CachedBlock lastBlock;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent
	 */
	public LastAccessExtentCache(Extent extent) {
		super(extent);
	}

	@Override
	public BaseBlock getLazyBlock(Vector position) {
		BlockVector blockVector = position.toBlockVector();
		CachedBlock lastBlock = this.lastBlock;
		if (lastBlock != null && lastBlock.position.equals(blockVector)) {
			return lastBlock.block;
		} else {
			BaseBlock block = super.getLazyBlock(position);
			this.lastBlock = new CachedBlock(blockVector, block);
			return block;
		}
	}

	@Override
	public boolean setBlock(Vector position, BaseBlock block) throws MinecraftMakerException {
		BlockVector blockVector = position.toBlockVector();
		CachedBlock lastBlock = this.lastBlock;
		if (lastBlock != null && lastBlock.position.equals(blockVector)) {
			// clear cache which might be changed now
			this.lastBlock = null;
		}
		return super.setBlock(position, block);
	}

	private static class CachedBlock {
		private final BlockVector position;
		private final BaseBlock block;

		private CachedBlock(BlockVector position, BaseBlock block) {
			this.position = position;
			this.block = block;
		}
	}

}
