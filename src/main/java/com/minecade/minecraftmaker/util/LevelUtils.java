package com.minecade.minecraftmaker.util;

import org.bukkit.World;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.bukkit.BukkitUtil;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.io.BlockArrayClipboard;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.world.CuboidRegion;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;

public class LevelUtils {

	public static Vector getLevelOrigin(short chunkZ) {
		short originX = 0;
		short originY = 0;
		short originZ = (short) (chunkZ * 16);
		return new Vector(originX, originY, originZ);
	}

	public static Region getLevelRegion(World world, short chunkZ) {
		Vector origin = getLevelOrigin(chunkZ);
		short width = 161;
		short height = 129;
		short length = 13;
		return new CuboidRegion(BukkitUtil.toWorld(world), origin, origin.add(width, height, length).subtract(Vector.ONE));
	}

	public static Clipboard createEmptyLevel(World world, short chunkZ, int floorBlockId) throws MinecraftMakerException {

		Region region = getLevelRegion(world, chunkZ);
		Vector minimumPoint = region.getMinimumPoint();
		Vector maximumPoint = region.getMaximumPoint();

		BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
		clipboard.setOrigin(minimumPoint);

		BaseBlock barrier = new BaseBlock(BlockID.BARRIER);
		BaseBlock darkGlass = new BaseBlock(BlockID.GLASS, 15);
		// construct the side walls
		for (int x = minimumPoint.getBlockX(); x <= region.getMaximumPoint().getBlockX(); x++) {
			for (int y = minimumPoint.getBlockY(); y <= region.getMaximumPoint().getBlockY(); y++) {
				// left side wall
				clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ()), barrier);
				clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ() + 1), (x % 16 == 0 && y % 16 == 0) ? darkGlass : barrier);
				clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ() + 2), barrier);
				// right side wall
				clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ()), barrier);
				clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ() - 1), (x % 16 == 0 && y % 16 == 0) ? darkGlass : barrier);
				clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ() - 2), barrier);
			}
		}
		// construct the back and end walls
		for (int y = minimumPoint.getBlockY(); y <= maximumPoint.getBlockY(); y++) {
			for (int z = minimumPoint.getBlockZ() + 3; z <= maximumPoint.getBlockZ() - 3; z++) {
				// triple back wall
				clipboard.setBlock(new Vector(minimumPoint.getBlockX(), y, z), barrier);
				clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 1, y, z), barrier);
				clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, y, z), barrier);
				// triple end wall
				clipboard.setBlock(new Vector(maximumPoint.getBlockX(), y, z), barrier);
				clipboard.setBlock(new Vector(maximumPoint.getBlockX() - 1, y, z), barrier);
				clipboard.setBlock(new Vector(maximumPoint.getBlockX() - 2, y, z), barrier);
			}
		}
		// construct the ceiling
		for (int x = minimumPoint.getBlockX() + 1; x < maximumPoint.getBlockX(); x++) {
			for (int z = minimumPoint.getBlockZ() + 3; z <= maximumPoint.getBlockZ() - 3; z++) {
				clipboard.setBlock(new Vector(x, maximumPoint.getBlockY(), z), barrier);
			}
		}
		// construct the floor (optional)
		if (floorBlockId > 0) {
			BaseBlock floorBlock = new BaseBlock(floorBlockId);
			clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, 64, minimumPoint.getBlockZ() + 6), floorBlock);
			for (int x = minimumPoint.getBlockX() + 3; x < maximumPoint.getBlockX() - 2; x++) {
				for (int z = minimumPoint.getBlockZ() + 3; z < maximumPoint.getBlockZ() - 2; z++) {
					clipboard.setBlock(new Vector(x, 64, z), floorBlock);
				}
			}
		}
		// floor block to start the level
		clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, 64, minimumPoint.getBlockZ() + 6), new BaseBlock(BlockID.BEACON));
		clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, 65, minimumPoint.getBlockZ() + 6), new BaseBlock(BlockID.AIR));
		clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, 66, minimumPoint.getBlockZ() + 6), new BaseBlock(BlockID.AIR));
		return clipboard;
	}

	public static Vector getLobbyOrigin() {
		short originX = -1;
		short originY = 63;
		short originZ = 0;
		return new Vector(originX, originY, originZ);
	}

	public static Region getLobbyRegion(World world) {
		Vector origin = getLobbyOrigin();
		short width = -64;
		short height = 65;
		short length = 64;
		return new CuboidRegion(BukkitUtil.toWorld(world), origin, origin.add(width, height, length).subtract(Vector.ONE));
	}

	public static Clipboard createLobbyFloor(World world) throws MinecraftMakerException {
		Region region = getLobbyRegion(world);
		Vector minimumPoint = region.getMinimumPoint();
		Vector maximumPoint = region.getMaximumPoint();

		BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
		clipboard.setOrigin(minimumPoint);

		// lobby floor
		BaseBlock barrier = new BaseBlock(BlockID.BARRIER);
		BaseBlock grass = new BaseBlock(BlockID.GRASS);
		for (int x = minimumPoint.getBlockX(); x < maximumPoint.getBlockX(); x++) {
			for (int z = minimumPoint.getBlockZ(); z < maximumPoint.getBlockZ(); z++) {
				clipboard.setBlock(new Vector(x, minimumPoint.getBlockY(), z), barrier);
				clipboard.setBlock(new Vector(x, minimumPoint.getBlockY() + 1, z), grass);
			}
		}
		BaseBlock darkGlass = new BaseBlock(BlockID.GLASS, 15);
		// construct the side walls
		for (int x = minimumPoint.getBlockX(); x <= region.getMaximumPoint().getBlockX(); x++) {
			for (int y = minimumPoint.getBlockY(); y <= region.getMaximumPoint().getBlockY(); y++) {
				// left side wall
				clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ()), barrier);
				clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ() + 1), (x % 16 == 0 && y % 16 == 0) ? darkGlass : barrier);
				clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ() + 2), barrier);
				// right side wall
				clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ()), barrier);
				clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ() - 1), (x % 16 == 0 && y % 16 == 0) ? darkGlass : barrier);
				clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ() - 2), barrier);
			}
		}
		// construct the back and end walls
		for (int y = minimumPoint.getBlockY(); y <= maximumPoint.getBlockY(); y++) {
			for (int z = minimumPoint.getBlockZ() + 3; z <= maximumPoint.getBlockZ() - 3; z++) {
				// triple back wall
				clipboard.setBlock(new Vector(minimumPoint.getBlockX(), y, z), barrier);
				clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 1, y, z), barrier);
				clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, y, z), barrier);
				// triple end wall
				clipboard.setBlock(new Vector(maximumPoint.getBlockX(), y, z), barrier);
				clipboard.setBlock(new Vector(maximumPoint.getBlockX() - 1, y, z), barrier);
				clipboard.setBlock(new Vector(maximumPoint.getBlockX() - 2, y, z), barrier);
			}
		}
		// construct the ceiling
		for (int x = minimumPoint.getBlockX() + 1; x < maximumPoint.getBlockX(); x++) {
			for (int z = minimumPoint.getBlockZ() + 3; z <= maximumPoint.getBlockZ() - 3; z++) {
				clipboard.setBlock(new Vector(x, maximumPoint.getBlockY(), z), barrier);
			}
		}

		return clipboard;
	}

	private LevelUtils() {
		super();
	}

}
