package com.minecade.minecraftmaker.schematic.world;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.entity.BaseEntity;

/**
 * Provides information on entities.
 */
public interface EntityRegistry {

    /**
     * Create a new entity using its ID.
     *
     * @param id the id
     * @return the entity, which may be null if the entity does not exist
     */
    @Nullable
    BaseEntity createFromId(String id);

}
