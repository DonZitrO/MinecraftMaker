package com.minecade.minecraftmaker.schematic.world;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

/**
 * Automatically loads chunks when blocks are accessed.
 */
public class ChunkLoadingExtent extends AbstractDelegateExtent {

	private final World world;
	private boolean enabled;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent
	 * @param world
	 *            the world
	 * @param enabled
	 *            true to enable
	 */
	public ChunkLoadingExtent(Extent extent, World world, boolean enabled) {
		super(extent);
		checkNotNull(world);
		this.enabled = enabled;
		this.world = world;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Create a new instance with chunk loading enabled.
	 *
	 * @param extent
	 *            the extent
	 * @param world
	 *            the world
	 */
	public ChunkLoadingExtent(Extent extent, World world) {
		this(extent, world, true);
	}

	@Override
	public boolean setBlock(Vector location, BaseBlock block) throws MinecraftMakerException {
		world.checkLoadedChunk(location);
		return super.setBlock(location, block);
	}

}
