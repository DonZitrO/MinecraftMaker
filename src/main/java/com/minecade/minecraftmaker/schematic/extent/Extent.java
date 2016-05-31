package com.minecade.minecraftmaker.schematic.extent;

import java.util.List;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.util.Location;
import com.minecade.minecraftmaker.schematic.world.InputExtent;
import com.minecade.minecraftmaker.schematic.world.OutputExtent;
import com.minecade.minecraftmaker.schematic.world.Region;
import com.minecade.minecraftmaker.schematic.world.Vector;

/**
 * A world, portion of a world, clipboard, or other object that can have blocks
 * set or entities placed.
 *
 * @see InputExtent the get____() portion
 * @see OutputExtent the set____() portion
 */
public interface Extent extends InputExtent, OutputExtent {

    /**
     * Get the minimum point in the extent.
     *
     * <p>If the extent is unbounded, then a large (negative) value may
     * be returned.</p>
     *
     * @return the minimum point
     */
    Vector getMinimumPoint();

    /**
     * Get the maximum point in the extent.
     *
     * <p>If the extent is unbounded, then a large (positive) value may
     * be returned.</p>
     *
     * @return the maximum point
     */
    Vector getMaximumPoint();

    /**
     * Get a list of all entities within the given region.
     *
     * <p>If the extent is not wholly loaded (i.e. a world being simulated in the
     * game will not have every chunk loaded), then this list may not be
     * incomplete.</p>
     *
     * @param region the region in which entities must be contained
     * @return a list of entities
     */
    List<? extends Entity> getEntities(Region region);

    /**
     * Get a list of all entities.
     *
     * <p>If the extent is not wholly loaded (i.e. a world being simulated in the
     * game will not have every chunk loaded), then this list may not be
     * incomplete.</p>
     *
     * @return a list of entities
     */
    List<? extends Entity> getEntities();

    /**
     * Create an entity at the given location.
     *
     * @param entity the entity
     * @param location the location
     * @return a reference to the created entity, or null if the entity could not be created
     */
    @Nullable Entity createEntity(Location location, BaseEntity entity);

}
