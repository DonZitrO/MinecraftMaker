package com.minecade.minecraftmaker.function.mask;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * A mask that tests whether given positions are contained within a region border
 */
public class RegionBorderMask extends AbstractMask {

	private Region region;

	/**
	 * Create a new region mask.
	 *
	 * @param region
	 *            the region
	 */
	public RegionBorderMask(Region region) {
		setRegion(region);
	}

	/**
	 * Get the region.
	 *
	 * @return the region
	 */
	public Region getRegion() {
		return region;
	}

	/**
	 * Set the region that positions must be contained within.
	 *
	 * @param region
	 *            the region
	 */
	public void setRegion(Region region) {
		checkNotNull(region);
		this.region = region.clone();
	}

	@Override
	public boolean test(Vector vector) {
		if (!region.contains(vector)) {
			return false;
		}
		if (vector.getBlockX() == region.getMinimumPoint().getBlockX()) {
			return true;
		}
		if (vector.getBlockY() == region.getMinimumPoint().getBlockY()) {
			return true;
		}
		if (vector.getBlockZ() == region.getMinimumPoint().getBlockZ()) {
			return true;
		}
		if (vector.getBlockX() == region.getMaximumPoint().getBlockX()) {
			return true;
		}
		if (vector.getBlockY() == region.getMaximumPoint().getBlockY()) {
			return true;
		}
		if (vector.getBlockZ() == region.getMaximumPoint().getBlockZ()) {
			return true;
		}
		return false;
	}

	@Nullable
	@Override
	public Mask2D toMask2D() {
		return null;
	}

}
