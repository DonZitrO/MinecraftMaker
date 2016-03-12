package com.minecade.minecraftmaker.exception;

/**
 * Thrown when too many blocks are changed (which may be limited due to the
 * configuration).
 */
public class MaxChangedBlocksException extends MinecraftMakerException {

	private static final long serialVersionUID = 1L;

	int maxBlocks;

	/**
	 * Create a new instance.
	 *
	 * @param maxBlocks
	 *            the maximum number of blocks that can be changed
	 */
	public MaxChangedBlocksException(int maxBlocks) {
		this.maxBlocks = maxBlocks;
	}

	/**
	 * Get the limit.
	 *
	 * @return the maximum number of blocks that can be changed
	 */
	public int getBlockLimit() {
		return maxBlocks;
	}

}
