package com.minecade.minecraftmaker.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public class LevelUtils {

	public static void placeEmptyLevel(World world, int chunkCoordinate) {
		int blockZ = chunkCoordinate * 16;
		int chunkLength = 10;
		int blockHeight = 64;
		int blockWidth = 7;
		// construct the back and end walls
		for (int y = 64; y < 64 + blockHeight + 1; y++) {
			for (int z = blockZ; z < blockZ + blockWidth + 2; z++) {
				Location loc = new Location(world, 0, y, z);
				Block block = loc.getBlock();
				block.setType(Material.STONE);
				block.getState().update();
				loc = new Location(world, 16 * chunkLength, y, z);
				block = loc.getBlock();
				block.setType(Material.DIAMOND_ORE);
				block.getState().update();
			}
		}
		// construct the side walls
		for(int x = 1; x < 16*chunkLength; x++) {
			for(int y = 64; y < 64 + blockHeight + 1; y++) {
				Location loc = new Location(world, x, y, blockZ);
				Block block = loc.getBlock();
				block.setType(Material.DIRT);
				block.getState().update();
				loc = new Location(world, x , y, blockZ + blockWidth + 1);
				block = loc.getBlock();
				block.setType(Material.REDSTONE_BLOCK);
				block.getState().update();
			}
		}
		// construct the ceiling
		for(int x = 0; x < 16*chunkLength; x++) {
			for(int z = blockZ; z < blockZ + blockWidth + 2; z++) {
				Location loc = new Location(world, x, 64 + blockHeight + 1, z);
				Block block = loc.getBlock();
				block.setType(Material.GLASS);
				block.getState().update();
			}
		}
	}

	private LevelUtils() {
		super();
	}

}
