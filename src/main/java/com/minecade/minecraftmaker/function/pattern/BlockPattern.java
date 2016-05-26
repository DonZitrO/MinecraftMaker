package com.minecade.minecraftmaker.function.pattern;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * A pattern that returns the same {@link BaseBlock} each time.
 */
public class BlockPattern extends AbstractPattern {

	private BaseBlock block;

	/**
	 * Create a new pattern with the given block.
	 *
	 * @param block
	 *            the block
	 */
	public BlockPattern(BaseBlock block) {
		setBlock(block);
	}

	/**
	 * Get the block.
	 *
	 * @return the block that is always returned
	 */
	public BaseBlock getBlock() {
		return block;
	}

	/**
	 * Set the block that is returned.
	 *
	 * @param block
	 *            the block
	 */
	public void setBlock(BaseBlock block) {
		checkNotNull(block);
		this.block = block;
	}

	@Override
	public BaseBlock apply(Vector position) {
		return block;
	}

}
