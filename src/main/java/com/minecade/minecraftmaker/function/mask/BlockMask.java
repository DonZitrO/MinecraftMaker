package com.minecade.minecraftmaker.function.mask;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * A mask that checks whether blocks at the given positions are matched by a
 * block in a list.
 *
 * <p>
 * This mask checks for both an exact block ID and data value match, as well for
 * a block with the same ID but a data value of -1.
 * </p>
 */
public class BlockMask extends AbstractExtentMask {

	private final Set<BaseBlock> blocks = new HashSet<BaseBlock>();

	/**
	 * Create a new block mask.
	 *
	 * @param extent
	 *            the extent
	 * @param blocks
	 *            a list of blocks to match
	 */
	public BlockMask(Extent extent, Collection<BaseBlock> blocks) {
		super(extent);
		checkNotNull(blocks);
		this.blocks.addAll(blocks);
	}

	/**
	 * Create a new block mask.
	 *
	 * @param extent
	 *            the extent
	 * @param block
	 *            an array of blocks to match
	 */
	public BlockMask(Extent extent, BaseBlock... block) {
		this(extent, Arrays.asList(checkNotNull(block)));
	}

	/**
	 * Add the given blocks to the list of criteria.
	 *
	 * @param blocks
	 *            a list of blocks
	 */
	public void add(Collection<BaseBlock> blocks) {
		checkNotNull(blocks);
		this.blocks.addAll(blocks);
	}

	/**
	 * Add the given blocks to the list of criteria.
	 *
	 * @param block
	 *            an array of blocks
	 */
	public void add(BaseBlock... block) {
		add(Arrays.asList(checkNotNull(block)));
	}

	/**
	 * Get the list of blocks that are tested with.
	 *
	 * @return a list of blocks
	 */
	public Collection<BaseBlock> getBlocks() {
		return blocks;
	}

	@Override
	public boolean test(Vector vector) {
		BaseBlock block = getExtent().getBlock(vector);
		return blocks.contains(block) || blocks.contains(new BaseBlock(block.getType(), -1));
	}

	@Nullable
	@Override
	public Mask2D toMask2D() {
		return null;
	}

}
