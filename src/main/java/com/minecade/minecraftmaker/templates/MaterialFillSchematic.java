package com.minecade.minecraftmaker.templates;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.minecade.minecraftmaker.MakerBase;
import com.minecade.minecraftmaker.MakerSchematic;
import com.minecade.minecraftmaker.SlotBoundaries;

public class MaterialFillSchematic implements MakerSchematic {
	
	private Material fillMaterial = Material.GRASS;
	private int data = 0;
	/**
	 * the length in chunks
	 */
	private int length = 10;
	/**
	 * The height in blocks
	 */
	private int height = 64;
	
	private int width = 7;
	/**
	 * How hight to fill with the filler block
	 */
	private int fillHeight = 10;

	public MaterialFillSchematic(Material mat, int data, int fillHeight) {
		fillMaterial = mat;
		this.data = data;
		this.fillHeight = fillHeight;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public boolean pasteSchematic(SlotBoundaries slot) {
		World arenaWorld = MakerBase.getMakerBase().getArenaWorld();
		//Construct the back and end walls
		for(int y = 0; y < height + 1; y++) {
			for(int z = 0; z < width + 2; z++) {
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
				loc = new Location(arenaWorld, x + slot.getX(), y, slot.getZ() + width + 1);
				block = loc.getBlock();
				block.setType(Material.BARRIER);
			}
		}
		//Construct the ceiling
		for(int x = 0; x < 16*length; x++) {
			for(int z = 0; z < width + 2; z++) {
				Location loc = new Location(arenaWorld, x + slot.getX(), height + 1, z + slot.getZ());
				Block block = loc.getBlock();
				block.setType(Material.BARRIER);
			}
		}
		//Fill the arena
		for(int x = 1; x < 16*length; x++) {
			for(int y = 0; y < fillHeight; y++) {
				for(int z = 1; z <= width; z++) {
					Location loc = new Location(arenaWorld, x + slot.getX(), y, z + slot.getZ());
					Block block = loc.getBlock();
					block.setType(fillMaterial);
					block.setData((byte)data);
				}
			}
		}
		return true;
	}

	public void setFillMaterial(Material fillMaterial) {
		this.fillMaterial = fillMaterial;
	}

	public void setData(int data) {
		this.data = data;
	}

	public void setFillHeight(int fillHeight) {
		this.fillHeight = fillHeight;
	}

}
