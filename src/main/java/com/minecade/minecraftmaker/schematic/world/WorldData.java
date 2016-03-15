package com.minecade.minecraftmaker.schematic.world;

/**
 * Describes the necessary data for blocks, entities, and other objects
 * on a world.
 */
public interface WorldData {

    /**
     * Get the block registry.
     *
     * @return the block registry
     */
    BlockRegistry getBlockRegistry();

    /**
     * Get the item registry.
     *
     * @return the item registry
     */
    ItemRegistry getItemRegistry();

    /**
     * Get the entity registry.
     *
     * @return the entity registry
     */
    EntityRegistry getEntityRegistry();

    /**
     * Get the biome registry.
     *
     * @return the biome registry
     */
    BiomeRegistry getBiomeRegistry();

}
