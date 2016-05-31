package com.minecade.minecraftmaker.function.visitor;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.function.RegionFunction;
import com.minecade.minecraftmaker.function.operation.LimitedTimeRunContext;
import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.BlockVector;
import com.minecade.minecraftmaker.schematic.world.Region;

/**
 * Utility class to apply region functions to
 * {@link com.minecade.minecraftmaker.schematic.world.Region}.
 */
public class ResumableRegionVisitor implements Operation {

	private final Iterator<BlockVector> regionIterator;
	private final RegionFunction function;
	private int affected = 0;
	private boolean firstRun = true;
	private long startNanoTime = 0;

	public ResumableRegionVisitor(Region region, RegionFunction function) {
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
	public Operation resume(LimitedTimeRunContext run) throws MinecraftMakerException {
		if (firstRun) {
			firstRun = false;
			startNanoTime = System.nanoTime();
		}
		while (regionIterator.hasNext() && run.shouldContinue()) {
			if (function.apply(regionIterator.next())) {
				affected++;
			}
		}
		if (regionIterator.hasNext()) {
			return this;
		}

		Bukkit.getLogger().info(String.format("ResumableRegionVisitor.resume - finished on: [%s] ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime)));
		return null;
	}

	@Override
	public void cancel() {
	}

	@Override
	public void addStatusMessages(List<String> messages) {
		messages.add(getAffected() + " blocks affected");
	}

}
