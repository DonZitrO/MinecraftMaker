package com.minecade.minecraftmaker.schematic.function;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.plugin.MinecraftMakerPlugin;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Executes several region functions in order.
 */
public class CombinedRegionFunction implements RegionFunction {

	private final List<RegionFunction> functions = new ArrayList<RegionFunction>();

	/**
	 * Create a combined region function.
	 */
	public CombinedRegionFunction() {
	}

	/**
	 * Create a combined region function.
	 *
	 * @param functions
	 *            a list of functions to match
	 */
	public CombinedRegionFunction(Collection<RegionFunction> functions) {
		checkNotNull(functions);
		this.functions.addAll(functions);
	}

	/**
	 * Create a combined region function.
	 *
	 * @param function
	 *            an array of functions to match
	 */
	public CombinedRegionFunction(RegionFunction... function) {
		this(Arrays.asList(checkNotNull(function)));
	}

	/**
	 * Add the given functions to the list of functions to call.
	 *
	 * @param functions
	 *            a list of functions
	 */
	public void add(Collection<RegionFunction> functions) {
		checkNotNull(functions);
		this.functions.addAll(functions);
	}

	/**
	 * Add the given functions to the list of functions to call.
	 *
	 * @param function
	 *            an array of functions
	 */
	public void add(RegionFunction... function) {
		add(Arrays.asList(checkNotNull(function)));
	}

	@Override
	public boolean apply(Vector position) throws MinecraftMakerException {
		long startNanoTime = 0;
		if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | CombinedRegionFunction.apply - position: [%s]", position));
			startNanoTime = System.nanoTime();
		}
		boolean ret = false;
		for (RegionFunction function : functions) {
			if (function.apply(position)) {
				ret = true;
			}
		}
		if (MinecraftMakerPlugin.getInstance().isDebugMode()) {
			Bukkit.getLogger().info(String.format("[DEBUG] | CombinedRegionFunction.apply - took: [%s] nanoseconds", System.nanoTime() - startNanoTime));
		}
		return ret;
	}

}
