package com.minecade.minecraftmaker.templates;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.minecade.minecraftmaker.MakerBase;
import com.minecade.minecraftmaker.MakerSchematic;
import com.minecade.minecraftmaker.SlotBoundaries;

public class BlankSchematic implements MakerSchematic {
	
	/**
	 * the length in chunks
	 */
	private int length = 10;
	/**
	 * The height in blocks
	 */
	private int height = 64;

	@Override
	public boolean pasteSchematic(SlotBoundaries slot) {
		World arenaWorld = MakerBase.getMakerBase().getArenaWorld();
		//Construct the back and end walls
		for(int y = 0; y < height + 1; y++) {
			for(int z = 0; z < 9; z++) {
				Location loc = new Location(arenaWorld, slot.getX(), y, z + slot.getZ());
				Block block = loc.getBlock();
				block.setType(Material.BARRIER);
				loc = new Location(arenaWorld, slot.getX() + 16*length, y, z + slot.getZ());
				block = loc.getBlock();
				block.setType(Material.BARRIER);
			}
		}
		//Construct the side walls
		for(int x = 1; x < 16*length; x++) {
			for(int y = 0; y < height + 1; y++) {
				Location loc = new Location(arenaWorld, x + slot.getX(), y, slot.getZ());
				Block block = loc.getBlock();
				block.setType(Material.BARRIER);
				loc = new Location(arenaWorld, x + slot.getX(), y, slot.getZ() + 8);
				block = loc.getBlock();
				block.setType(Material.BARRIER);
			}
		}
		//Construct the ceiling
		for(int x = 0; x < 16*length; x++) {
			for(int z = 0; z < 9; z++) {
				Location loc = new Location(arenaWorld, x + slot.getX(), height + 1, z + slot.getZ());
				Block block = loc.getBlock();
				block.setType(Material.BARRIER);
			}
		}
		return true;
	}

}
