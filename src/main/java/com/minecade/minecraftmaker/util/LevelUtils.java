package com.minecade.minecraftmaker.util;

import org.bukkit.World;

import com.minecade.minecraftmaker.bukkit.BukkitUtil;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.io.BlockArrayClipboard;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.world.CuboidRegion;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;

public class LevelUtils {

	public static Clipboard createEmptyLevel(World world, short chunkCoordinate) throws MinecraftMakerException {

		short originX = 0;
		short originY = 64;
		short originZ = (short) (chunkCoordinate * 16);

		short width = 128;
		short height = 64;
		short length = 9;

		Vector origin;
		Region region;

		origin = new Vector(originX, originY, originZ);
		region = new CuboidRegion(origin, origin.add(width, height, length).subtract(Vector.ONE));
		region.setWorld(BukkitUtil.toWorld(world));

		BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
		clipboard.setOrigin(origin);

		// construct the back and end walls
		for (int y = origin.getBlockY(); y < region.getMaximumPoint().getBlockY(); y++) {
			for (int z = origin.getBlockZ(); z < region.getMaximumPoint().getBlockZ(); z++) {
				clipboard.setBlock(new Vector(origin.getBlockX(), y, z), new BaseBlock(BlockID.STONE));
				clipboard.setBlock(new Vector(region.getMaximumPoint().getBlockX(), y, z), new BaseBlock(BlockID.DIAMOND_ORE));
			}
		}
		// construct the side walls
		for(int x = origin.getBlockX(); x < region.getMaximumPoint().getBlockX(); x++) {
			for(int y = origin.getBlockY(); y < region.getMaximumPoint().getBlockY(); y++) {
				clipboard.setBlock(new Vector(x, y, origin.getBlockZ()), new BaseBlock(BlockID.DIRT));
				clipboard.setBlock(new Vector(x, y, region.getMaximumPoint().getBlockZ()), new BaseBlock(BlockID.REDSTONE_BLOCK));
			}
		}
		// construct the ceiling
		for(int x = origin.getBlockX(); x < region.getMaximumPoint().getBlockX(); x++) {
			for(int z = origin.getBlockZ(); z < region.getMaximumPoint().getBlockZ(); z++) {
				clipboard.setBlock(new Vector(x, region.getMaximumPoint().getBlockY(), z), new BaseBlock(BlockID.GLASS));
			}
		}
		return clipboard;
	}

	private LevelUtils() {
		super();
	}

}
