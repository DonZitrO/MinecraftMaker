package com.minecade.minecraftmaker.schematic.bukkit;

import javax.annotation.Nullable;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;

import com.minecade.minecraftmaker.schematic.block.BaseBlock;
import com.minecade.minecraftmaker.schematic.entity.BaseEntity;

/**
 * An interface for adapters of various Bukkit implementations.
 */
public interface BukkitImplAdapter {

	/**
	 * Get the block ID for the given material.
	 *
	 * <p>
	 * Returns 0 if it is not known or it doesn't exist.
	 * </p>
	 *
	 * @param material
	 *            the material
	 * @return the block ID
	 */
	int getBlockId(Material material);

	/**
	 * Get the material for the given block ID.
	 *
	 * <p>
	 * Returns {@link Material#AIR} if it is not known or it doesn't exist.
	 * </p>
	 *
	 * @param id
	 *            the block ID
	 * @return the material
	 */
	Material getMaterial(int id);

	/**
	 * Get the biome ID for the given biome.
	 *
	 * <p>
	 * Returns 0 if it is not known or it doesn't exist.
	 * </p>
	 *
	 * @param biome
	 *            biome
	 * @return the biome ID
	 */
	int getBiomeId(Biome biome);

	/**
	 * Get the biome ID for the given biome ID..
	 *
	 * <p>
	 * Returns {@link Biome#OCEAN} if it is not known or it doesn't exist.
	 * </p>
	 *
	 * @param id
	 *            the biome ID
	 * @return the biome
	 */
	Biome getBiome(int id);

	/**
	 * Get the block at the given location.
	 *
	 * @param location
	 *            the location
	 * @return the block
	 */
	BaseBlock getBlock(Location location);

	/**
	 * Set the block at the given location.
	 *
	 * @param location
	 *            the location
	 * @param state
	 *            the block
	 * @param notifyAndLight
	 *            notify and light if set
	 * @return true if a block was likely changed
	 */
	boolean setBlock(Location location, BaseBlock state, boolean notifyAndLight);

	/**
	 * Get the state for the given entity.
	 *
	 * @param entity
	 *            the entity
	 * @return the state, or null
	 */
	@Nullable
	BaseEntity getEntity(Entity entity);

	/**
	 * Create the given entity.
	 *
	 * @param location
	 *            the location
	 * @param state
	 *            the state
	 * @return the created entity or null
	 */
	@Nullable
	Entity createEntity(Location location, BaseEntity state);

}
