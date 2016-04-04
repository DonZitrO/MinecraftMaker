package com.minecade.minecraftmaker.function.visitor;

import java.util.List;

import com.minecade.minecraftmaker.function.RegionFunction;
import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.function.operation.RunContext;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Utility class to apply region functions to
 * {@link com.minecade.minecraftmaker.schematic.world.Region}.
 */
public class RegionVisitor implements Operation {

	private final Region region;
	private final RegionFunction function;
	private int affected = 0;

	public RegionVisitor(Region region, RegionFunction function) {
		this.region = region;
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
		for (Vector pt : region) {
			if (function.apply(pt)) {
				affected++;
			}
		}

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
