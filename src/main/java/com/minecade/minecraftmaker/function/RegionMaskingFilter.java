package com.minecade.minecraftmaker.function;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.function.mask.Mask;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * Passes calls to {@link #apply(com.sk89q.worldedit.Vector)} to the delegate
 * {@link com.sk89q.worldedit.function.RegionFunction} if they match the given
 * mask.
 */
public class RegionMaskingFilter implements RegionFunction {

	private final RegionFunction function;
	private Mask mask;

	/**
	 * Create a new masking filter.
	 *
	 * @param mask
	 *            the mask
	 * @param function
	 *            the function
	 */
	public RegionMaskingFilter(Mask mask, RegionFunction function) {
		checkNotNull(function);
		checkNotNull(mask);
		this.mask = mask;
		this.function = function;
	}

	@Override
	public boolean apply(Vector position) throws MinecraftMakerException {
		return mask.test(position) && function.apply(position);
	}

}
