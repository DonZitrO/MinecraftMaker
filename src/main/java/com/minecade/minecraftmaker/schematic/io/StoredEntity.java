package com.minecade.minecraftmaker.schematic.io;

import static com.google.common.base.Preconditions.checkNotNull;

import com.minecade.minecraftmaker.schematic.entity.BaseEntity;
import com.minecade.minecraftmaker.schematic.entity.Entity;
import com.minecade.minecraftmaker.schematic.extent.Extent;
import com.minecade.minecraftmaker.schematic.util.Location;


/**
 * An implementation of {@link Entity} that stores a {@link BaseEntity} with it.
 *
 * <p>Calls to {@link #getState()} return a clone.</p>
 */
abstract class StoredEntity implements Entity {

    private final Location location;
    private final BaseEntity entity;

    /**
     * Create a new instance.
     *
     * @param location the location
     * @param entity the entity (which will be copied)
     */
    StoredEntity(Location location, BaseEntity entity) {
        checkNotNull(location);
        checkNotNull(entity);
        this.location = location;
        this.entity = new BaseEntity(entity);
    }

    /**
     * Get the entity state. This is not a copy.
     *
     * @return the entity
     */
    BaseEntity getEntity() {
        return entity;
    }

    @Override
    public BaseEntity getState() {
        return new BaseEntity(entity);
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public Extent getExtent() {
        return location.getExtent();
    }

}
