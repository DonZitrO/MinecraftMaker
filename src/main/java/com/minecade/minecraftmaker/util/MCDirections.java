package com.minecade.minecraftmaker.util;

import com.minecade.minecraftmaker.schematic.world.Direction;

/**
 * Utility methods for working with directions in Minecraft.
 */
public final class MCDirections {

	public static Direction fromHanging(int i) {
		switch (i) {
		case 0:
			return Direction.SOUTH;
		case 1:
			return Direction.WEST;
		case 2:
			return Direction.NORTH;
		case 3:
			return Direction.EAST;
		default:
			return Direction.NORTH;
		}
	}

	public static int toHanging(Direction direction) {
		switch (direction) {
		case SOUTH:
			return 0;
		case WEST:
			return 1;
		case NORTH:
			return 2;
		case EAST:
			return 3;
		default:
			return 0;
		}
	}

	public static int fromLegacyHanging(byte i) {
		switch (i) {
		case 0:
			return 2;
		case 1:
			return 1;
		case 2:
			return 0;
		default:
			return 3;
		}
	}

	public static byte toLegacyHanging(int i) {
		switch (i) {
		case 0:
			return (byte) 2;
		case 1:
			return (byte) 1;
		case 2:
			return (byte) 0;
		default:
			return (byte) 3;
		}
	}

	private MCDirections() {
		super();
	}

}
