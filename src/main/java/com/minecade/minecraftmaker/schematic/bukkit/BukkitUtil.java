package com.minecade.minecraftmaker.schematic.bukkit;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Dye;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.block.BlockID;
import com.minecade.minecraftmaker.schematic.block.SkullBlock;
import com.minecade.minecraftmaker.schematic.exception.MinecraftMakerException;
import com.minecade.minecraftmaker.schematic.exception.NotABlockException;
import com.minecade.minecraftmaker.schematic.util.Location;
import com.minecade.minecraftmaker.schematic.world.BlockType;
import com.minecade.minecraftmaker.schematic.world.BlockVector;
import com.minecade.minecraftmaker.schematic.world.ItemID;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.World;

public final class BukkitUtil {

	public static World toWorld(org.bukkit.World w) {
		return new BukkitWorld(w);
	}

	public static org.bukkit.World toWorld(final World world) {
		return ((BukkitWorld) world).getWorld();
	}

	public static BlockVector toVector(Block block) {
		return new BlockVector(block.getX(), block.getY(), block.getZ());
	}

	public static BlockVector toVector(BlockFace face) {
		return new BlockVector(face.getModX(), face.getModY(), face.getModZ());
	}

	public static Vector toVector(org.bukkit.Location loc) {
		return new Vector(loc.getX(), loc.getY(), loc.getZ());
	}

	public static Location toLocation(org.bukkit.Location loc) {
		return new Location(toWorld(loc.getWorld()), new Vector(loc.getX(), loc.getY(), loc.getZ()), loc.getYaw(), loc.getPitch());
	}

	public static Vector toVector(org.bukkit.util.Vector vector) {
		return new Vector(vector.getX(), vector.getY(), vector.getZ());
	}

	public static org.bukkit.Location toLocation(org.bukkit.World world, Vector pt) {
		return new org.bukkit.Location(world, pt.getX(), pt.getY(), pt.getZ());
	}

	public static org.bukkit.Location center(org.bukkit.Location loc) {
		return new org.bukkit.Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 0.5, loc.getBlockZ() + 0.5, loc.getPitch(), loc.getYaw());
	}

	public static Player matchSinglePlayer(Server server, String name) {
		List<Player> players = server.matchPlayer(name);
		if (players.isEmpty()) {
			return null;
		}
		return players.get(0);
	}

	/**
	 * Bukkit's Location class has serious problems with floating point
	 * precision.
	 */
	public static boolean equals(org.bukkit.Location a, org.bukkit.Location b) {
		if (Math.abs(a.getX() - b.getX()) > EQUALS_PRECISION)
			return false;
		if (Math.abs(a.getY() - b.getY()) > EQUALS_PRECISION)
			return false;
		if (Math.abs(a.getZ() - b.getZ()) > EQUALS_PRECISION)
			return false;
		return true;
	}

	public static final double EQUALS_PRECISION = 0.0001;

//	public static org.bukkit.Location toLocation(Location location) {
//		Vector pt = location.getPosition();
//		return new org.bukkit.Location(toWorld(location.getWorld()), pt.getX(), pt.getY(), pt.getZ(), location.getYaw(), location.getPitch());
//	}

//	public static BukkitEntity toLocalEntity(Entity e) {
//		switch (e.getType()) {
//		case EXPERIENCE_ORB:
//			return new BukkitExpOrb(toLocation(e.getLocation()), e.getUniqueId(), ((ExperienceOrb) e).getExperience());
//		case PAINTING:
//			Painting paint = (Painting) e;
//			return new BukkitPainting(toLocation(e.getLocation()), paint.getArt(), paint.getFacing(), e.getUniqueId());
//		case DROPPED_ITEM:
//			return new BukkitItem(toLocation(e.getLocation()), ((Item) e).getItemStack(), e.getUniqueId());
//		default:
//			return new BukkitEntity(toLocation(e.getLocation()), e.getType(), e.getUniqueId());
//		}
//	}

	public static BaseBlock toBlock(World world, ItemStack itemStack) throws MinecraftMakerException {
		final int typeId = itemStack.getTypeId();

		switch (typeId) {
		case ItemID.INK_SACK:
			final Dye materialData = (Dye) itemStack.getData();
			if (materialData.getColor() == DyeColor.BROWN) {
				return new BaseBlock(BlockID.COCOA_PLANT, -1);
			}
			break;

		case ItemID.HEAD:
			return new SkullBlock(0, (byte) itemStack.getDurability());

		default:
			final BaseBlock baseBlock = BlockType.getBlockForItem(typeId, itemStack.getDurability());
			if (baseBlock != null) {
				return baseBlock;
			}
			break;
		}

		if (world.isValidBlockType(typeId)) {
			return new BaseBlock(typeId, -1);
		}

		throw new NotABlockException(typeId);
	}

	private BukkitUtil() {
		super();
	}

}
