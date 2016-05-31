package com.minecade.minecraftmaker.schematic.extent;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.BlockType;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.World;

/**
 * Handles various quirks when setting blocks, such as ice turning into water or
 * containers dropping their contents.
 */
public class BlockQuirkExtent extends AbstractDelegateExtent {

	private final World world;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent
	 * @param world
	 *            the world
	 */
	public BlockQuirkExtent(Extent extent, World world) {
		super(extent);
		checkNotNull(world);
		this.world = world;
	}

	@Override
	public boolean setBlock(Vector position, BaseBlock block) throws MinecraftMakerException {
		BaseBlock lazyBlock = getExtent().getLazyBlock(position);
		int existing = lazyBlock.getType();

		if (BlockType.isContainerBlock(existing)) {
			world.clearContainerBlockContents(position); // Clear the container block so that it doesn't drop items
		} else if (existing == BlockID.ICE) {
			world.setBlock(position, new BaseBlock(BlockID.AIR)); // Ice turns until water so this has to be done first
		}

		return super.setBlock(position, block);
	}

}
