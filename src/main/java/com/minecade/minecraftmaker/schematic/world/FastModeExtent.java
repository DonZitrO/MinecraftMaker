package com.minecade.minecraftmaker.schematic.world;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.function.operation.RunContext;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

/**
 * Implements "fast mode" which may skip physics, lighting, etc.
 */
public class FastModeExtent extends AbstractDelegateExtent {

	private final World world;
	private final Set<BlockVector2D> dirtyChunks = new HashSet<BlockVector2D>();
	private boolean enabled = true;

	/**
	 * Create a new instance with fast mode enabled.
	 *
	 * @param world
	 *            the world
	 */
	public FastModeExtent(World world) {
		this(world, true);
	}

	/**
	 * Create a new instance.
	 *
	 * @param world
	 *            the world
	 * @param enabled
	 *            true to enable fast mode
	 */
	public FastModeExtent(World world, boolean enabled) {
		super(world);
		checkNotNull(world);
		this.world = world;
		this.enabled = enabled;
	}

	/**
	 * Return whether fast mode is enabled.
	 *
	 * @return true if fast mode is enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Set fast mode enable status.
	 *
	 * @param enabled
	 *            true to enable fast mode
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean setBlock(Vector location, BaseBlock block) throws MinecraftMakerException {
		if (enabled) {
			dirtyChunks.add(new BlockVector2D(location.getBlockX() >> 4, location.getBlockZ() >> 4));
			return world.setBlock(location, block, false);
		} else {
			return world.setBlock(location, block, true);
		}
	}

	@Override
	protected Operation commitBefore() {
		return new Operation() {
			@Override
			public Operation resume(RunContext run) throws MinecraftMakerException {
				if (!dirtyChunks.isEmpty()) {
					world.fixAfterFastMode(dirtyChunks);
				}
				return null;
			}

			@Override
			public void cancel() {
			}

			@Override
			public void addStatusMessages(List<String> messages) {
			}
		};
	}

}
