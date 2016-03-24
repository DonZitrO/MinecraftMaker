package com.minecade.minecraftmaker.bukkit;

import com.minecade.minecraftmaker.schematic.world.BiomeRegistry;
import com.minecade.minecraftmaker.schematic.world.LegacyWorldData;

/**
 * World data for the Bukkit platform.
 */
class BukkitWorldData extends LegacyWorldData {

	private static final BukkitWorldData INSTANCE = new BukkitWorldData();
	private final BiomeRegistry biomeRegistry = new BukkitBiomeRegistry();

	/**
	 * Create a new instance.
	 */
	BukkitWorldData() {
	}

	@Override
	public BiomeRegistry getBiomeRegistry() {
		return biomeRegistry;
	}

	/**
	 * Get a static instance.
	 *
	 * @return an instance
	 */
	public static BukkitWorldData getInstance() {
		return INSTANCE;
	}

}
