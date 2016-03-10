package com.minecade.minecraftmaker;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import com.minecade.core.gamebase.MinigameLocation;

public class MakerRelativeLocation {
	
	double x, y, z;
	float yaw = 0, pitch = 0;
	
	/**
	 * Creates a new MinigameLocation based off the given Location object
	 * @param loc The object to take data from
	 */
	public MakerRelativeLocation(Location loc, SlotBoundaries slot) {
		x = loc.getX() - slot.getX();
		y = loc.getY();
		z = loc.getZ() - slot.getZ();
		yaw = loc.getYaw();
		pitch = loc.getPitch();
	}
	
	/**
	 * Creates a new  MinigameLocation using the world name and X, Y, Z coordinated
	 * @param world The name of the world this location should be in
	 * @param x The X coordinate
	 * @param y The Y coordinate
	 * @param z The Z coordinate
	 */
	public MakerRelativeLocation(double x, double y, double z, SlotBoundaries slot) {
		this.x = x - slot.getX();
		this.y = y;
		this.z = z - slot.getZ();
	}
	
	/**
	 * Creates a new MinigameLocation using world name, 3D coordinates, and direction/heading
	 * @param world The name of the world this location should be in
	 * @param x The X coordinate
	 * @param y The Y (vertical) coordinate
	 * @param z The Z coordinate
	 * @param yaw The "left and right" turn value
	 * @param pitch The "up and down" face value
	 */
	public MakerRelativeLocation(double x, double y, double z, float yaw, float pitch, SlotBoundaries slot) {
		this.x = x - slot.getX();
		this.y = y;
		this.z = z - slot.getZ();
		this.yaw = yaw;
		this.pitch = pitch;
	}
	
	/**
	 * Gets the location, or null if it cannot resolve the world.
	 * This gets around the problem of loading the configs before
	 * the worlds are actually loaded.
	 * @return
	 */
	public Location getLocation(SlotBoundaries slot) {
		Location loc = null;
		World wworld = MakerBase.getMakerBase().getArenaWorld();
		if(wworld != null) {
			loc = new Location(wworld, x + slot.getX(), y, z + slot.getZ(), yaw, pitch);
		}
		return loc;
	}

	/**
	 * Gets the stored X coordinate
	 * @return X position
	 */
	public double getX() {
		return x;
	}
	
	/**
	 * Gets the X coordinate of the block at this location
	 * @return An int for the block's X coordinate
	 */
	public int getBlockX() {
		return Location.locToBlock(x);
	}

	/**
	 * Gets the stored Y coordinate
	 * @return Y position
	 */
	public double getY() {
		return y;
	}
	
	/**
	 * Gets the Y coordinate of the block at this location
	 * @return An int for the block's Y coordinate
	 */
	public int getBlockY() {
		return Location.locToBlock(y);
	}

	/**
	 * Gets the stored Z coordinate
	 * @return Z position
	 */
	public double getZ() {
		return z;
	}
	
	/**
	 * Gets the Z coordinate of the block at this location
	 * @return An int for the block's Z coordinate
	 */
	public int getBlockZ() {
		return Location.locToBlock(z);
	}

	/**
	 * Gets the yaw stored in this coordinate
	 * @return The stored yaw. 0 will always be returned if no yaw was stored
	 */
	public float getYaw() {
		return yaw;
	}

	/**
	 * Gets the pitch stored in this coordinate
	 * @return The stored pitch. 0 will always be returned if no yaw was stored
	 */
	public float getPitch() {
		return pitch;
	}
	
	/**
	 * Gets the String representation of this location
	 * The result can be fed into fromString() to create an object with the same values
	 * @return A String representation
	 */
	@Override
	public String toString() {
		return getX() + "," + getY() + "," + getZ() + "," + getPitch() + "," + getYaw();
	}
	
	/**
	 * Gets a String representation of the location of the block found at this location
	 * @return The block location
	 */
	public String blockToString() {
		return getBlockX() + "," + getBlockY() + "," + getBlockZ();
	}
	
	/**
	 * Creates a new MinigameLocation based off of the given String
	 * @param location The String to convert
	 * @return A new MinigameLocation object, or null if the String could not be parsed
	 */
	public static MakerRelativeLocation fromString(String location) {
		// Sanity check for null
		if (location == null) {
			return null;
		}
		
		// Begin the parsing
		String[] split = location.split(",");
		if(split.length > 2) {
			try {
				double x = Double.parseDouble(split[0]);
				double y = Double.parseDouble(split[1]);
				double z = Double.parseDouble(split[2]);
				if(split.length == 5) {
					float pitch = Float.parseFloat(split[3]);
					float yaw = Float.parseFloat(split[4]);
					return new MakerRelativeLocation(x, y, z, yaw, pitch, new SlotBoundaries(0));
				}else {
					return new MakerRelativeLocation(x, y, z, new SlotBoundaries(0));
				}
			}catch(NumberFormatException e) {

			}
		}
		return null;
	}
}
