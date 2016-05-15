package com.minecade.minecraftmaker.level;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Material;
import org.bukkit.material.MaterialData;

import com.minecade.minecraftmaker.schematic.world.BlockVector;

public class LevelRedstoneInteraction {

	private final BlockVector location;
	private final Material material;
	private final MaterialData materialData;
	private final long tick;
	private final int oldCurrent;
	private final int newCurrent;

	public LevelRedstoneInteraction(BlockVector location, Material material, MaterialData materialData, long tick, int oldCurrent,int newCurrent) {
		checkNotNull(location);
		checkNotNull(material);
		checkNotNull(materialData);
		this.location = location;
		this.material = material;
		this.materialData = materialData.clone();
		this.tick = tick;
		this.oldCurrent = oldCurrent;
		this.newCurrent = newCurrent;
	}

	public BlockVector getLocation() {
		return location;
	}

	public Material getMaterial() {
		return material;
	}

	public MaterialData getMaterialData() {
		return materialData.clone();
	}

	public long getTick() {
		return tick;
	}

	public int getOldCurrent() {
		return oldCurrent;
	}

	public int getNewCurrent() {
		return newCurrent;
	}

	@Override
	public String toString() {
		return "LevelRedstoneInteraction [location=" + location + ", material=" + material + ", materialData=" + materialData + ", tick=" + tick + ", oldCurrent=" + oldCurrent + ", newCurrent=" + newCurrent + "]";
	}

}
