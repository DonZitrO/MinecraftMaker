package com.minecade.minecraftmaker.util;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.minecade.mcore.function.block.BlockReplace;
import com.minecade.mcore.function.operation.Operation;
import com.minecade.mcore.function.operation.ResumableForwardExtentCopy;
import com.minecade.mcore.function.pattern.BlockPattern;
import com.minecade.mcore.function.visitor.ResumableRegionVisitor;
import com.minecade.mcore.schematic.block.BaseBlock;
import com.minecade.mcore.schematic.block.BlockID;
import com.minecade.mcore.schematic.extent.Extent;
import com.minecade.mcore.schematic.io.BlockArrayClipboard;
import com.minecade.mcore.schematic.io.Clipboard;
import com.minecade.mcore.schematic.transform.Identity;
import com.minecade.mcore.schematic.transform.Transform;
import com.minecade.mcore.schematic.world.BlockTransformExtent;
import com.minecade.mcore.schematic.world.CuboidRegion;
import com.minecade.mcore.schematic.world.Region;
import com.minecade.mcore.schematic.world.Vector;
import com.minecade.mcore.schematic.world.WorldData;
import com.minecade.minecraftmaker.level.MakerPlayableLevel;

public class LevelUtils {

	public static final Transform IDENTITY_TRANSFORM = new Identity();

	private static Clipboard createEmptyClipboard(Region region) {
		checkNotNull(region);
		BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
		clipboard.setOrigin(region.getMinimumPoint());
		return clipboard;
	}

	public static Operation createRegionFacesOperation(Extent destination, CuboidRegion region, BaseBlock block) {
		Region faces = region.getFaces();
		BlockReplace replace = new BlockReplace(destination, new BlockPattern(block));
		return new ResumableRegionVisitor(faces, replace);
	}

	public static Clipboard createLevelStartClipboard(short chunkZ) {
		Vector levelOrigin = getLevelOrigin(chunkZ);
		Vector startOrigin = levelOrigin.add(2, MakerPlayableLevel.FLOOR_LEVEL_Y, 6);
		CuboidRegion startRegion = new CuboidRegion(startOrigin, startOrigin.add(0, 2, 0));
		BlockArrayClipboard clipboard = new BlockArrayClipboard(startRegion);
		clipboard.setOrigin(startOrigin);
		BaseBlock beacon = new BaseBlock(BlockID.BEACON);
		BaseBlock air = new BaseBlock(BlockID.AIR);
		clipboard.setBlock(startOrigin, beacon);
		clipboard.setBlock(startOrigin.add(0, 1, 0), air);
		clipboard.setBlock(startOrigin.add(0, 2, 0), air);
		return clipboard;
	}

	public static Clipboard createEmptyLevelClipboard(short chunkZ, int floorBlockId) {
		Region region = getLevelRegion(chunkZ, MakerPlayableLevel.DEFAULT_LEVEL_WIDTH, MakerPlayableLevel.DEFAULT_LEVEL_HEIGHT);

		Vector minimumPoint = region.getMinimumPoint();
		Vector maximumPoint = region.getMaximumPoint();

		BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
		clipboard.setOrigin(minimumPoint);

		BaseBlock barrier = new BaseBlock(BlockID.BARRIER);
		//BaseBlock darkGlass = new BaseBlock(BlockID.GLASS, 15);
		// construct the side walls
		for (int x = minimumPoint.getBlockX(); x <= region.getMaximumPoint().getBlockX(); x++) {
			for (int y = minimumPoint.getBlockY(); y <= region.getMaximumPoint().getBlockY(); y++) {
				// left side wall
				clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ()), barrier);
				clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ() + 1), /*(x % 16 == 0 && y % 16 == 0) ? darkGlass : */barrier);
				clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ() + 2), barrier);
				// right side wall
				clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ()), barrier);
				clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ() - 1), /*(x % 16 == 0 && y % 16 == 0) ? darkGlass : */barrier);
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
				clipboard.setBlock(new Vector(x, maximumPoint.getBlockY() - 1, z), barrier);
				clipboard.setBlock(new Vector(x, maximumPoint.getBlockY() - 2, z), barrier);
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
			clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, MakerPlayableLevel.FLOOR_LEVEL_Y, minimumPoint.getBlockZ() + 6), floorBlock);
			for (int x = minimumPoint.getBlockX() + 3; x < maximumPoint.getBlockX() - 2; x++) {
				for (int z = minimumPoint.getBlockZ() + 3; z < maximumPoint.getBlockZ() - 2; z++) {
					clipboard.setBlock(new Vector(x, MakerPlayableLevel.FLOOR_LEVEL_Y, z), floorBlock);
				}
			}
		}
		// floor block to start the level
		clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, MakerPlayableLevel.FLOOR_LEVEL_Y, minimumPoint.getBlockZ() + 6), new BaseBlock(BlockID.BEACON));
		clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, MakerPlayableLevel.FLOOR_LEVEL_Y + 1, minimumPoint.getBlockZ() + 6), new BaseBlock(BlockID.AIR));
		clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, MakerPlayableLevel.FLOOR_LEVEL_Y + 2, minimumPoint.getBlockZ() + 6), new BaseBlock(BlockID.AIR));
		return clipboard;
	}

	public static Clipboard createLevelRemainingWidthEmptyClipboard(short chunkZ, int regionWidth) {
		CuboidRegion remainingRegion = getLevelRegion(chunkZ, MakerPlayableLevel.MAX_LEVEL_WIDTH, MakerPlayableLevel.MAX_LEVEL_HEIGHT);
		if (regionWidth >= remainingRegion.getWidth()) {
			return null;
		}
		Bukkit.getLogger().severe(String.format("createLevelRemainingWidthEmptyClipboard - 1: [%s] - 2: [%s]", remainingRegion.getMinimumPoint(), remainingRegion.getMaximumPoint()));
		remainingRegion.contract(new Vector(regionWidth, 0, 0));
		Bukkit.getLogger().severe(String.format("createLevelRemainingWidthEmptyClipboard - 1: [%s] - 2: [%s]", remainingRegion.getMinimumPoint(), remainingRegion.getMaximumPoint()));
		return createEmptyClipboard(remainingRegion);
	}

	public static Clipboard createLevelRemainingHeightEmptyClipboard(short chunkZ, int regionHeight) {
		CuboidRegion remainingRegion = getLevelRegion(chunkZ, MakerPlayableLevel.MAX_LEVEL_WIDTH, MakerPlayableLevel.MAX_LEVEL_HEIGHT);
		if (regionHeight >= remainingRegion.getHeight()) {
			return null;
		}
		Bukkit.getLogger().severe(String.format("createLevelRemainingHeightEmptyClipboard - 1: [%s] - 2: [%s]", remainingRegion.getMinimumPoint(), remainingRegion.getMaximumPoint()));
		remainingRegion.contract(new Vector(0, regionHeight, 0));
		Bukkit.getLogger().severe(String.format("createLevelRemainingHeightEmptyClipboard - 1: [%s] - 2: [%s]", remainingRegion.getMinimumPoint(), remainingRegion.getMaximumPoint()));
		return createEmptyClipboard(remainingRegion);
	}

	public static Operation createPasteOperation(Clipboard clipboard, Extent destination, WorldData worldData) {
		BlockTransformExtent extent = new BlockTransformExtent(clipboard, IDENTITY_TRANSFORM, worldData.getBlockRegistry());
		ResumableForwardExtentCopy copy = new ResumableForwardExtentCopy(extent, clipboard.getRegion(), destination, clipboard.getOrigin());
		copy.setTransform(IDENTITY_TRANSFORM);
		// FIXME: review this at some point
		//copy.setSourceMask(Masks.negate(new RegionBorderMask(clipboard.getRegion())));
		return copy;
	}

	public static Vector getLevelOrigin(short chunkZ) {
		short originX = 0;
		short originY = 0;
		short originZ = (short) (chunkZ * 16);
		return new Vector(originX, originY, originZ);
	}

	public static CuboidRegion getLevelRegion(short chunkZ, int levelWidth, int levelHeight) {
		Vector origin = getLevelOrigin(chunkZ);
		int width = levelWidth;
		int height = levelHeight;
		int length = 13;
		return new CuboidRegion(origin, origin.add(width, height, length).subtract(Vector.ONE));
	}

	public static short getLocationSlot(org.bukkit.Location location) {
		if (location.getY() >= MakerPlayableLevel.MAX_LEVEL_HEIGHT) {
			return -1;
		}
		if (location.getBlockX() < 0 || location.getBlockX() > MakerPlayableLevel.MAX_LEVEL_WIDTH) {
			return -1;
		}
		int slot = (location.getBlockZ() >> 4);
		if (slot < 0 || slot > Short.MAX_VALUE) {
			return -1;
		}
		return (short) slot;
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

	public static boolean isAboveLocation(org.bukkit.util.Vector location, org.bukkit.util.Vector base) {
		checkNotNull(location);
		if (base == null) {
			return false;
		}
		if (location.getBlockX() == base.getBlockX() && location.getBlockZ() == base.getBlockZ() && location.getBlockY() > base.getBlockY()) {
			return true;
		}
		return false;
	}

	public static boolean isValidEndBeaconLocation(org.bukkit.util.Vector location, Region region) {
		checkNotNull(location);
		if (region == null) {
			return false;
		}
		if (location.getBlockY() < 2 || location.getBlockY() + 4 >= region.getMaximumPoint().getBlockY()) {
			return false;
		}
		if (location.getBlockX() - 3 <= region.getMinimumPoint().getBlockX()|| location.getBlockX() + 3 >= region.getMaximumPoint().getBlockX()) {
			return false;
		}
		if (location.getBlockZ() - 3 <= region.getMinimumPoint().getBlockZ()|| location.getBlockZ() + 3 >= region.getMaximumPoint().getBlockZ()) {
			return false;
		}
		return true;
	}

	// public static Clipboard createLobbyClipboard(World world) throws
	// MinecraftMakerException {
	// Region region = getLobbyRegion(world);
	// Vector minimumPoint = region.getMinimumPoint();
	// Vector maximumPoint = region.getMaximumPoint();
	//
	// BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
	// clipboard.setOrigin(minimumPoint);
	//
	// // lobby floor
	// BaseBlock barrier = new BaseBlock(BlockID.BARRIER);
	// BaseBlock grass = new BaseBlock(BlockID.GRASS);
	// for (int x = minimumPoint.getBlockX(); x < maximumPoint.getBlockX(); x++)
	// {
	// for (int z = minimumPoint.getBlockZ(); z < maximumPoint.getBlockZ(); z++)
	// {
	// clipboard.setBlock(new Vector(x, minimumPoint.getBlockY(), z), barrier);
	// clipboard.setBlock(new Vector(x, minimumPoint.getBlockY() + 1, z),
	// grass);
	// }
	// }
	// BaseBlock darkGlass = new BaseBlock(BlockID.GLASS, 15);
	// // construct the side walls
	// for (int x = minimumPoint.getBlockX(); x <=
	// region.getMaximumPoint().getBlockX(); x++) {
	// for (int y = minimumPoint.getBlockY(); y <=
	// region.getMaximumPoint().getBlockY(); y++) {
	// // left side wall
	// clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ()), barrier);
	// clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ() + 1), (x %
	// 16 == 0 && y % 16 == 0) ? darkGlass : barrier);
	// clipboard.setBlock(new Vector(x, y, minimumPoint.getBlockZ() + 2),
	// barrier);
	// // right side wall
	// clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ()), barrier);
	// clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ() - 1), (x %
	// 16 == 0 && y % 16 == 0) ? darkGlass : barrier);
	// clipboard.setBlock(new Vector(x, y, maximumPoint.getBlockZ() - 2),
	// barrier);
	// }
	// }
	// // construct the back and end walls
	// for (int y = minimumPoint.getBlockY(); y <= maximumPoint.getBlockY();
	// y++) {
	// for (int z = minimumPoint.getBlockZ() + 3; z <= maximumPoint.getBlockZ()
	// - 3; z++) {
	// // triple back wall
	// clipboard.setBlock(new Vector(minimumPoint.getBlockX(), y, z), barrier);
	// clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 1, y, z),
	// barrier);
	// clipboard.setBlock(new Vector(minimumPoint.getBlockX() + 2, y, z),
	// barrier);
	// // triple end wall
	// clipboard.setBlock(new Vector(maximumPoint.getBlockX(), y, z), barrier);
	// clipboard.setBlock(new Vector(maximumPoint.getBlockX() - 1, y, z),
	// barrier);
	// clipboard.setBlock(new Vector(maximumPoint.getBlockX() - 2, y, z),
	// barrier);
	// }
	// }
	// // construct the ceiling
	// for (int x = minimumPoint.getBlockX() + 1; x < maximumPoint.getBlockX();
	// x++) {
	// for (int z = minimumPoint.getBlockZ() + 3; z <= maximumPoint.getBlockZ()
	// - 3; z++) {
	// clipboard.setBlock(new Vector(x, maximumPoint.getBlockY(), z), barrier);
	// }
	// }
	// return clipboard;
	// }

	// public static Vector getLobbyOrigin() {
	// short originX = -1;
	// short originY = FLOOR_LEVEL_Y - 1;
	// short originZ = -1;
	// return new Vector(originX, originY, originZ);
	// }
	//
	// public static Region getLobbyRegion(World world) {
	// Vector origin = getLobbyOrigin();
	// short width = -18;
	// short height = 50;
	// short length = 166;
	// return new CuboidRegion(BukkitUtil.toWorld(world), origin,
	// origin.add(width, height, length).subtract(Vector.ONE));
	// }

	private LevelUtils() {
		super();
	}

}
