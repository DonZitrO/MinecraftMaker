package com.minecade.minecraftmaker.schematic.world;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.extent.AbstractDelegateExtent;
import com.minecade.minecraftmaker.schematic.extent.Extent;

/**
 * Makes changes to the world as if a player had done so during survival mode.
 *
 * <p>
 * Note that this extent may choose to not call the underlying extent and may
 * instead call methods on the {@link World} that is passed in the constructor.
 * For that reason, if you wish to "catch" changes, you should catch them before
 * the changes reach this extent.
 * </p>
 */
public class SurvivalModeExtent extends AbstractDelegateExtent {

	private final World world;
	private boolean toolUse = false;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent
	 * @param world
	 *            the world
	 */
	public SurvivalModeExtent(Extent extent, World world) {
		super(extent);
		checkNotNull(world);
		this.world = world;
	}

	/**
	 * Return whether changes to the world should be simulated with the use of
	 * game tools (such as pickaxes) whenever possible and reasonable.
	 *
	 * <p>
	 * For example, we could pretend that the act of setting a coal ore block to
	 * air (nothing) was the act of a player mining that coal ore block with a
	 * pickaxe, which would mean that a coal item would be dropped.
	 * </p>
	 *
	 * @return true if tool use is to be simulated
	 */
	public boolean hasToolUse() {
		return toolUse;
	}

	/**
	 * Set whether changes to the world should be simulated with the use of game
	 * tools (such as pickaxes) whenever possible and reasonable.
	 *
	 * @param toolUse
	 *            true if tool use is to be simulated
	 * @see #hasToolUse() for an explanation
	 */
	public void setToolUse(boolean toolUse) {
		this.toolUse = toolUse;
	}

	@Override
	public boolean setBlock(Vector location, BaseBlock block) throws MinecraftMakerException {
		if (toolUse && block.getType() == BlockID.AIR) {
			world.simulateBlockMine(location);
			return true;
		} else {
			return super.setBlock(location, block);
		}
	}

}
