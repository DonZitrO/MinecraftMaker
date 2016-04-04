package com.minecade.minecraftmaker.schematic.world;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.function.mask.BlockMask;
import com.minecade.minecraftmaker.function.mask.Mask;
import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BaseItem;
import com.minecade.minecraftmaker.schematic.block.BaseItemStack;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;

/**
 * An abstract implementation of {@link World}.
 */
public abstract class AbstractWorld implements World {

	@Override
	public boolean useItem(Vector position, BaseItem item, Direction face) {
		return false;
	}

	@Override
	public final boolean setBlock(Vector pt, BaseBlock block) throws MinecraftMakerException {
		return setBlock(pt, block, true);
	}

	@Override
	public int getMaxY() {
		return getMaximumPoint().getBlockY();
	}

	@Override
	public boolean isValidBlockType(int type) {
		return BlockType.fromID(type) != null;
	}

	@Override
	public boolean usesBlockData(int type) {
		// We future proof here by assuming all unknown blocks use data
		return BlockType.usesData(type) || BlockType.fromID(type) == null;
	}

	@Override
	public Mask createLiquidMask() {
		return new BlockMask(this, new BaseBlock(BlockID.STATIONARY_LAVA, -1), new BaseBlock(BlockID.LAVA, -1), new BaseBlock(BlockID.STATIONARY_WATER, -1), new BaseBlock(BlockID.WATER, -1));
	}

	@Override
	public void dropItem(Vector pt, BaseItemStack item, int times) {
		for (int i = 0; i < times; ++i) {
			dropItem(pt, item);
		}
	}

	@Override
	public void simulateBlockMine(Vector pt) {
		BaseBlock block = getLazyBlock(pt);
		BaseItemStack stack = BlockType.getBlockDrop(block.getId(), (short) block.getData());

		if (stack != null) {
			final int amount = stack.getAmount();
			if (amount > 1) {
				dropItem(pt, new BaseItemStack(stack.getType(), 1, stack.getData()), amount);
			} else {
				dropItem(pt, stack, amount);
			}
		}

		try {
			setBlock(pt, new BaseBlock(BlockID.AIR));
		} catch (MinecraftMakerException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void checkLoadedChunk(Vector pt) {
	}

	@Override
	public void fixAfterFastMode(Iterable<BlockVector2D> chunks) {
	}

	@Override
	public void fixLighting(Iterable<BlockVector2D> chunks) {
	}

	@Override
	public boolean playEffect(Vector position, int type, int data) {
		return false;
	}

	@Override
	public Vector getMinimumPoint() {
		return new Vector(-30000000, 0, -30000000);
	}

	@Override
	public Vector getMaximumPoint() {
		return new Vector(30000000, 255, 30000000);
	}

	@Override
	public @Nullable Operation commit() {
		return null;
	}

}
