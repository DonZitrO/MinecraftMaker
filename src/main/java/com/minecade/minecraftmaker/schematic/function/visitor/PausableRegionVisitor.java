package com.minecade.minecraftmaker.schematic.function.visitor;

import java.util.Iterator;
import java.util.List;

import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.function.RegionFunction;
import com.minecade.minecraftmaker.schematic.function.operation.Operation;
import com.minecade.minecraftmaker.schematic.function.operation.RunContext;
import com.minecade.minecraftmaker.schematic.world.BlockVector;
import com.minecade.minecraftmaker.schematic.world.Region;

/**
 * Utility class to apply region functions to
 * {@link com.minecade.minecraftmaker.schematic.world.Region}.
 */
public class PausableRegionVisitor implements Operation {

	private final Iterator<BlockVector> regionIterator;
	private final RegionFunction function;
	private int affected = 0;

	public PausableRegionVisitor(Region region, RegionFunction function) {
		this.regionIterator = region.iterator();
		this.function = function;
	}

	/**
	 * Get the number of affected objects.
	 *
	 * @return the number of affected
	 */
	public int getAffected() {
		return affected;
	}

	@Override
	public Operation resume(RunContext run) throws MinecraftMakerException {
		while (regionIterator.hasNext() && run.shouldContinue()) {
			if (function.apply(regionIterator.next())) {
				affected++;
			}
		}
		if (regionIterator.hasNext()) {
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
		messages.add(getAffected() + " blocks affected");
	}

}
