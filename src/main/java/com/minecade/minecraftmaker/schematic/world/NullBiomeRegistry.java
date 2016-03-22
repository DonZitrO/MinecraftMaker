package com.minecade.minecraftmaker.schematic.world;

import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * A biome registry that knows nothing.
 */
public class NullBiomeRegistry implements BiomeRegistry {

	/**
	 * Create a new instance.
	 */
	public NullBiomeRegistry() {
	}

	@Nullable
	@Override
	public BaseBiome createFromId(int id) {
		return null;
	}

	@Override
	public List<BaseBiome> getBiomes() {
		return Collections.emptyList();
	}

	@Nullable
	@Override
	public BiomeData getData(BaseBiome biome) {
		return null;
	}

}
