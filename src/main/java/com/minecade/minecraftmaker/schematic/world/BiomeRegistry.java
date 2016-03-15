package com.minecade.minecraftmaker.schematic.world;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Provides information on biomes.
 */
public interface BiomeRegistry {

    /**
     * Create a new biome given its biome ID.
     *
     * @param id its biome ID
     * @return a new biome or null if it can't be created
     */
    @Nullable
    BaseBiome createFromId(int id);

    /**
     * Get a list of available biomes.
     *
     * @return a list of biomes
     */
    List<BaseBiome> getBiomes();

    /**
     * Get data about a biome.
     *
     * @param biome the biome
     * @return a data object or null if information is not known
     */
    @Nullable
    BiomeData getData(BaseBiome biome);

}
