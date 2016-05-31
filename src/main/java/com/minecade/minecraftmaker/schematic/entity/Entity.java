package com.minecade.minecraftmaker.schematic.entity;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.schematic.extent.Extent;
import com.minecade.minecraftmaker.schematic.util.Faceted;
import com.minecade.minecraftmaker.schematic.util.Location;

/**
 * A reference to an instance of an entity that exists in an {@link Extent}
 * and thus would have position and similar details.
 *
 * <p>This object cannot be directly cloned because it represents a particular
 * instance of an entity, but a {@link BaseEntity} can be created from
 * this entity by calling {@link #getState()}.</p>
 */
public interface Entity extends Faceted {

    /**
     * Get a copy of the entity's state.
     *
     * <p>In some cases, this method may return {@code null} if a snapshot
     * of the entity can't be created. It may not be possible, for example,
     * to get a snapshot of a player.</p>
     *
     * @return the entity's state or null if one cannot be created
     */
    @Nullable
    BaseEntity getState();

    /**
     * Get the location of this entity.
     *
     * @return the location of the entity
     */
    Location getLocation();

    /**
     * Get the extent that this entity is on.
     *
     * @return the extent
     */
    Extent getExtent();

    /**
     * Remove this entity from it container.
     *
     * @return true if removal was successful
     */
    boolean remove();

}
