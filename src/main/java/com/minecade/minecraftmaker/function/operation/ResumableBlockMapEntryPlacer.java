package com.minecade.minecraftmaker.function.operation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.BlockVector;
import com.minecade.minecraftmaker.schematic.world.Extent;

/**
 * Sets block from an iterator of {@link Map.Entry} containing a
 * {@link BlockVector} as the key and a {@link BaseBlock} as the value.
 */
public class ResumableBlockMapEntryPlacer implements Operation {

	private final Extent extent;
	private final Iterator<Map.Entry<BlockVector, BaseBlock>> iterator;

	/**
	 * Create a new instance.
	 *
	 * @param extent
	 *            the extent to set the blocks on
	 * @param iterator
	 *            the iterator
	 */
	public ResumableBlockMapEntryPlacer(Extent extent, Iterator<Map.Entry<BlockVector, BaseBlock>> iterator) {
		checkNotNull(extent);
		checkNotNull(iterator);
		this.extent = extent;
		this.iterator = iterator;
	}

	@Override
	public Operation resume(LimitedTimeRunContext run) throws MinecraftMakerException {
		while (iterator.hasNext() && run.shouldContinue()) {
			Map.Entry<BlockVector, BaseBlock> entry = iterator.next();
			extent.setBlock(entry.getKey(), entry.getValue());
		}

		if (iterator.hasNext()) {
			return this;
		} else {
			return null;
		}
	}

	@Override
	public void cancel() {
	}

	@Override
	public void addStatusMessages(List<String> messages) {
	}

}
