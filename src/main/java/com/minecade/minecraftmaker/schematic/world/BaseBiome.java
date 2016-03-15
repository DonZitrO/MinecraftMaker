package com.minecade.minecraftmaker.schematic.world;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Basic storage object to represent a given biome.
 */
public class BaseBiome {

    private int id;

    /**
     * Create a new biome with the given biome ID.
     *
     * @param id the biome ID
     */
    public BaseBiome(int id) {
        this.id = id;
    }

    /**
     * Create a clone of the given biome.
     *
     * @param biome the biome to clone
     */
    public BaseBiome(BaseBiome biome) {
        checkNotNull(biome);
        this.id = biome.getId();
    }

    /**
     * Get the biome ID.
     *
     * @return the biome ID
     */
    public int getId() {
        return id;
    }

    /**
     * Set the biome id.
     *
     * @param id the biome ID
     */
    public void setId(int id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseBiome baseBiome = (BaseBiome) o;

        return id == baseBiome.id;
    }

    @Override
    public int hashCode() {
        return id;
    }
}
