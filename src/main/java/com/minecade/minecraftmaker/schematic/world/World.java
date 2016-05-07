package com.minecade.minecraftmaker.schematic.world;

import com.minecade.minecraftmaker.function.mask.Mask;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BaseItem;
import com.minecade.minecraftmaker.schematic.block.BaseItemStack;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

/**
 * Represents a world (dimension).
 */
public interface World extends Extent {

	/**
	 * Get the name of the world.
	 *
	 * @return a name for the world
	 */
	String getName();

	/**
	 * Get the maximum Y.
	 *
	 * @return the maximum Y
	 */
	int getMaxY();

	/**
	 * Checks whether the given block ID is a valid block ID.
	 *
	 * @param id
	 *            the block ID
	 * @return true if the block ID is a valid one
	 */
	boolean isValidBlockType(int id);

	/**
	 * Checks whether the given block ID uses data values for differentiating
	 * types of blocks.
	 *
	 * @param id
	 *            the block ID
	 * @return true if the block uses data values
	 */
	boolean usesBlockData(int id);

	/**
	 * Create a mask that matches all liquids.
	 *
	 * <p>
	 * Implementations should override this so that custom liquids are
	 * supported.
	 * </p>
	 *
	 * @return a mask
	 */
	Mask createLiquidMask();

	/**
	 * Use the given item on the block at the given location on the given side.
	 *
	 * @param item
	 *            The item
	 * @param face
	 *            The face
	 * @return Whether it succeeded
	 */
	boolean useItem(Vector position, BaseItem item, Direction face);

	/**
	 * Similar to {@link Extent#setBlock(Vector, BaseBlock)} but a
	 * {@code notifyAndLight} parameter indicates whether adjacent blocks should
	 * be notified that changes have been made and lighting operations should be
	 * executed.
	 *
	 * <p>
	 * If it's not possible to skip lighting, or if it's not possible to avoid
	 * notifying adjacent blocks, then attempt to meet the specification as best
	 * as possible.
	 * </p>
	 *
	 * <p>
	 * On implementations where the world is not simulated, the
	 * {@code notifyAndLight} parameter has no effect either way.
	 * </p>
	 *
	 * @param position
	 *            position of the block
	 * @param block
	 *            block to set
	 * @param notifyAndLight
	 *            true to to notify and light
	 * @return true if the block was successfully set (return value may not be
	 *         accurate)
	 */
	boolean setBlock(Vector position, BaseBlock block, boolean notifyAndLight) throws MinecraftMakerException;

	/**
	 * Get the light level at the given block.
	 *
	 * @param position
	 *            the position
	 * @return the light level (0-15)
	 */
	int getBlockLightLevel(Vector position);

	/**
	 * Clear a chest's contents.
	 *
	 * @param position
	 *            the position
	 * @return true if the container was cleared
	 */
	boolean clearContainerBlockContents(Vector position);

	/**
	 * Drop an item at the given position.
	 *
	 * @param position
	 *            the position
	 * @param item
	 *            the item to drop
	 * @param count
	 *            the number of individual stacks to drop (number of item
	 *            entities)
	 */
	void dropItem(Vector position, BaseItemStack item, int count);

	/**
	 * Drop one stack of the item at the given position.
	 *
	 * @param position
	 *            the position
	 * @param item
	 *            the item to drop
	 * @see #dropItem(Vector, BaseItemStack, int) shortcut method to specify the
	 *      number of stacks
	 */
	void dropItem(Vector position, BaseItemStack item);

	/**
	 * Simulate a block being mined at the given position.
	 *
	 * @param position
	 *            the position
	 */
	void simulateBlockMine(Vector position);

	/**
	 * Load the chunk at the given position if it isn't loaded.
	 *
	 * @param position
	 *            the position
	 */
	void checkLoadedChunk(Vector position);

	/**
	 * Fix the given chunks after fast mode was used.
	 *
	 * <p>
	 * Fast mode makes calls to {@link #setBlock(Vector, BaseBlock, boolean)}
	 * with {@code false} for the {@code notifyAndLight} parameter, which may
	 * causes lighting errors to accumulate. Use of this method, if it is
	 * implemented by the underlying world, corrects those lighting errors and
	 * may trigger block change notifications.
	 * </p>
	 *
	 * @param chunks
	 *            a list of chunk coordinates to fix
	 */
	void fixAfterFastMode(Iterable<BlockVector2D> chunks);

	/**
	 * Relight the given chunks if possible.
	 *
	 * @param chunks
	 *            a list of chunk coordinates to fix
	 */
	void fixLighting(Iterable<BlockVector2D> chunks);

	/**
	 * Play the given effect.
	 *
	 * @param position
	 *            the position
	 * @param type
	 *            the effect type
	 * @param data
	 *            the effect data
	 * @return true if the effect was played
	 */
	boolean playEffect(Vector position, int type, int data);

	/**
	 * Get the data for blocks and so on for this world.
	 *
	 * @return the world data
	 */
	WorldData getWorldData();

	@Override
	boolean equals(Object other);

	@Override
	int hashCode();

}
