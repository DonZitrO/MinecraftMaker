package com.minecade.minecraftmaker;

/**
 * This defines a slot. Each slot is made programmatically
 * on the fly and released back into a pool of available slots.
 * Each slot should extend outward on the X axis. X = length, Z = width
 * @author joshua
 *
 */
public class SlotBoundaries {

	int x = 0;
	int z = 0;
	
	/**
	 * Initialize a new slot. Each slot ID should be numeric starting from 0
	 * @param slotid
	 */
	public SlotBoundaries(int slotid) {
		z = slotid*16;
	}
	
	/**
	 * Gets the lower X axis slot boundary
	 * @return
	 */
	public int getX() {
		return x;
	}
	
	/**
	 * Gets the lower Z axis slot boundary
	 * @return
	 */
	public int getZ() {
		return z;
	}
}
