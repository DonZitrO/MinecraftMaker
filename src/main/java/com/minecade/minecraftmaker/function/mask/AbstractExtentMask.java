package com.minecade.minecraftmaker.function.mask;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.schematic.extent.Extent;

/**
 * An abstract implementation of {@link Mask} that takes uses an {@link Extent}.
 */
public abstract class AbstractExtentMask extends AbstractMask {

	private Extent extent;

	/**
	 * Construct a new mask.
	 *
	 * @param extent
	 *            the extent
	 */
	protected AbstractExtentMask(Extent extent) {
		setExtent(extent);
	}

	/**
	 * Get the extent.
	 *
	 * @return the extent
	 */
	public Extent getExtent() {
		return extent;
	}

	/**
	 * Set the extent.
	 *
	 * @param extent
	 *            the extent
	 */
	public void setExtent(Extent extent) {
		checkNotNull(extent);
		this.extent = extent;
	}

}
