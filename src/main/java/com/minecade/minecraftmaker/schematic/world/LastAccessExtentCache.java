package com.minecade.minecraftmaker.schematic.world;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;

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

	private static class CachedBlock {
		private final BlockVector position;
		private final BaseBlock block;

		private CachedBlock(BlockVector position, BaseBlock block) {
			this.position = position;
			this.block = block;
		}
	}

}
