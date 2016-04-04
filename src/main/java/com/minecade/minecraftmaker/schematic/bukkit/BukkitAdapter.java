package com.minecade.minecraftmaker.schematic.bukkit;

import static com.google.common.base.Preconditions.checkNotNull;

import org.bukkit.Bukkit;

import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.util.Location;
import com.minecade.minecraftmaker.schematic.world.Vector;
import com.minecade.minecraftmaker.schematic.world.World;


/**
 * Adapts between Bukkit and WorldEdit equivalent objects.
 */
final class BukkitAdapter {

    /**
     * Convert any WorldEdit world into an equivalent wrapped Bukkit world.
     *
     * <p>If a matching world cannot be found, a {@link RuntimeException}
     * will be thrown.</p>
     *
     * @param world the world
     * @return a wrapped Bukkit world
     */
    public static BukkitWorld asBukkitWorld(World world) {
        if (world instanceof BukkitWorld) {
            return (BukkitWorld) world;
        } else {
            throw new RuntimeException("World '" + world.getName() + "' has no matching version in Bukkit");
        }
    }

    /**
     * Create a WorldEdit world from a Bukkit world.
     *
     * @param world the Bukkit world
     * @return a WorldEdit world
     */
    public static World adapt(org.bukkit.World world) {
        checkNotNull(world);
        return new BukkitWorld(world);
    }

    /**
     * Create a Bukkit world from a WorldEdit world.
     *
     * @param world the WorldEdit world
     * @return a Bukkit world
     */
    public static org.bukkit.World adapt(World world) {
        checkNotNull(world);
        if (world instanceof BukkitWorld) {
            return ((BukkitWorld) world).getWorld();
        } else {
            org.bukkit.World match = Bukkit.getServer().getWorld(world.getName());
            if (match != null) {
                return match;
            } else {
                throw new IllegalArgumentException("Can't find a Bukkit world for " + world);
            }
        }
    }

    /**
     * Create a WorldEdit location from a Bukkit location.
     *
     * @param location the Bukkit location
     * @return a WorldEdit location
     */
    public static Location adapt(org.bukkit.Location location) {
        checkNotNull(location);
        Vector position = BukkitUtil.toVector(location);
        return new Location(
                adapt(location.getWorld()),
                position,
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Create a Bukkit location from a WorldEdit location.
     *
     * @param location the WorldEdit location
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(Location location) {
        checkNotNull(location);
        Vector position = location.toVector();
        return new org.bukkit.Location(
                adapt((World) location.getExtent()),
                position.getX(), position.getY(), position.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Create a Bukkit location from a WorldEdit position with a Bukkit world.
     *
     * @param world the Bukkit world
     * @param position the WorldEdit position
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(org.bukkit.World world, Vector position) {
        checkNotNull(world);
        checkNotNull(position);
        return new org.bukkit.Location(
                world,
                position.getX(), position.getY(), position.getZ());
    }

    /**
     * Create a Bukkit location from a WorldEdit location with a Bukkit world.
     *
     * @param world the Bukkit world
     * @param location the WorldEdit location
     * @return a Bukkit location
     */
    public static org.bukkit.Location adapt(org.bukkit.World world, Location location) {
        checkNotNull(world);
        checkNotNull(location);
        return new org.bukkit.Location(
                world,
                location.getX(), location.getY(), location.getZ(),
                location.getYaw(),
                location.getPitch());
    }

    /**
     * Create a WorldEdit entity from a Bukkit entity.
     *
     * @param entity the Bukkit entity
     * @return a WorldEdit entity
     */
    public static Entity adapt(org.bukkit.entity.Entity entity) {
        checkNotNull(entity);
        return new BukkitEntity(entity);
    }

	private BukkitAdapter() {
		super();
	}

}
