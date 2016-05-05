package com.minecade.minecraftmaker.util;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.minecade.minecraftmaker.function.mask.ExistingBlockMask;
import com.minecade.minecraftmaker.function.operation.Operation;
import com.minecade.minecraftmaker.function.operation.ResumableForwardExtentCopy;
import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.bukkit.BukkitUtil;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.io.BlockArrayClipboard;
import com.minecade.minecraftmaker.schematic.io.Clipboard;
import com.minecade.minecraftmaker.schematic.transform.Identity;
import com.minecade.minecraftmaker.schematic.transform.Transform;
import com.minecade.minecraftmaker.schematic.world.BlockTransformExtent;
import com.minecade.minecraftmaker.schematic.world.CuboidRegion;
import com.minecade.minecraftmaker.schematic.world.Extent;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.WorldData;

public class LevelUtils {

	public static final Transform IDENTITY_TRANSFORM = new Identity();

	private static final int HIGHEST_LEVEL_Y = 63;
	private static final int FLOOR_LEVEL_Y = 16;
	private static final int DEFAULT_LEVEL_WIDTH_CHUNKS = 5;
	private static final int MAX_LEVEL_WIDTH_CHUNKS = 10;
	private static final int MAX_LEVELS_PER_WORLD = 10;

	public static Clipboard createEmptyLevelClipboard(World world, short chunkZ, int floorBlockId) throws MinecraftMakerException {

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
		// construct the bottom
		for (int x = minimumPoint.getBlockX() + 1; x < maximumPoint.getBlockX(); x++) {
			for (int z = minimumPoint.getBlockZ() + 3; z <= maximumPoint.getBlockZ() - 3; z++) {
				clipboard.setBlock(new Vector(x, minimumPoint.getBlockY(), z), barrier);
			}
		}
		// construct the floor (optional)
		if (floorBlockId > 0) {
			BaseBlock floorBlock = new BaseBlock(floorBlockId);
			clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, FLOOR_LEVEL_Y, minimumPoint.getBlockZ() + 6), floorBlock);
			for (int x = minimumPoint.getBlockX() + 3; x < maximumPoint.getBlockX() - 2; x++) {
				for (int z = minimumPoint.getBlockZ() + 3; z < maximumPoint.getBlockZ() - 2; z++) {
					clipboard.setBlock(new Vector(x, FLOOR_LEVEL_Y, z), floorBlock);
				}
			}
		}
		// floor block to start the level
		clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, FLOOR_LEVEL_Y, minimumPoint.getBlockZ() + 6), new BaseBlock(BlockID.BEACON));
		clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, FLOOR_LEVEL_Y + 1, minimumPoint.getBlockZ() + 6), new BaseBlock(BlockID.AIR));
		clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, FLOOR_LEVEL_Y + 2, minimumPoint.getBlockZ() + 6), new BaseBlock(BlockID.AIR));
		return clipboard;
	}

	public static Clipboard createLobbyClipboard(World world) throws MinecraftMakerException {
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

	public static Operation createPasteOperation(Clipboard clipboard, Extent destination, WorldData worldData) {
		BlockTransformExtent extent = new BlockTransformExtent(clipboard, IDENTITY_TRANSFORM, worldData.getBlockRegistry());
		ResumableForwardExtentCopy copy = new ResumableForwardExtentCopy(extent, clipboard.getRegion(), destination, clipboard.getOrigin());
		copy.setTransform(IDENTITY_TRANSFORM);
		boolean ignoreAirBlocks = false;
		if (ignoreAirBlocks) {
			copy.setSourceMask(new ExistingBlockMask(clipboard));
		}
		return copy;
	}

	public static Vector getLevelOrigin(short chunkZ) {
		short originX = 0;
		short originY = 0;
		short originZ = (short) (chunkZ * 16);
		return new Vector(originX, originY, originZ);
	}

	public static Region getLevelRegion(World world, short chunkZ) {
		Vector origin = getLevelOrigin(chunkZ);
		short width = DEFAULT_LEVEL_WIDTH_CHUNKS * 16;
		short height = 66;
		short length = 13;
		return new CuboidRegion(BukkitUtil.toWorld(world), origin, origin.add(width, height, length).subtract(Vector.ONE));
	}

	public static Vector getLobbyOrigin() {
		short originX = -1;
		short originY = FLOOR_LEVEL_Y - 1;
		short originZ = -1;
		return new Vector(originX, originY, originZ);
	}

	public static Region getLobbyRegion(World world) {
		Vector origin = getLobbyOrigin();
		short width = -18;
		short height = 50;
		short length = 166;
		return new CuboidRegion(BukkitUtil.toWorld(world), origin, origin.add(width, height, length).subtract(Vector.ONE));
	}

	public static short getLocationSlot(org.bukkit.Location location) {
		if (location.getY() > HIGHEST_LEVEL_Y) {
			return -1;
		}
		if (location.getBlockX() < 0 || location.getBlockX() > MAX_LEVEL_WIDTH_CHUNKS * 16) {
			return -1;
		}
		if (location.getBlockZ() < 0 || location.getBlockZ() > MAX_LEVELS_PER_WORLD * 16) {
			return -1;
		}
		return (short) (location.getBlockZ() / 16);
	}

	public static boolean isBeaconPowerBlock(Block block) {
		if (!block.getType().equals(Material.IRON_BLOCK)) {
			return false;
		}
		Block blockAbove = block.getRelative(BlockFace.UP);
		if (blockAbove.getType().equals(Material.BEACON)) {
			return true;
		}
		for (BlockFace around : BlockFace.values()) {
			switch (around) {
			case NORTH_WEST:
			case NORTH:
			case NORTH_EAST:
			case WEST:
			case EAST:
			case SOUTH_WEST:
			case SOUTH:
			case SOUTH_EAST:
				if (blockAbove.getRelative(around).getType().equals(Material.BEACON)) {
					return true;
				}
				break;
			default:
				break;
			}
		}
		return false;
	}

	private LevelUtils() {
		super();
	}

}
