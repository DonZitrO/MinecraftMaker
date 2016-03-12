package com.minecade.minecraftmaker.entity;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.jnbt.CompoundTag;
import com.minecade.minecraftmaker.jnbt.NbtValued;

/**
 * Represents a mutable "snapshot" of an entity.
 *
 * <p>An instance of this class contains all the information needed to
 * accurately reproduce the entity, provided that the instance was
 * made correctly. In some implementations, it may not be possible to get a
 * snapshot of entities correctly, so, for example, the NBT data for an entity
 * may be missing.</p>
 *
 * <p>This class identifies entities using its entity type string, although
 * this is not very efficient as the types are currently not interned. This
 * may be changed in the future.</p>
 */
public class BaseEntity implements NbtValued {

    private String id;
    private CompoundTag nbtData;

    /**
     * Create a new base entity.
     *
     * @param id the entity type ID
     * @param nbtData NBT data
     */
    public BaseEntity(String id, CompoundTag nbtData) {
        setTypeId(id);
        setNbtData(nbtData);
    }

    /**
     * Create a new base entity with no NBT data.
     *
     * @param id the entity type ID
     */
    public BaseEntity(String id) {
        setTypeId(id);
    }

    /**
     * Make a clone of a {@link BaseEntity}.
     *
     * @param other the object to clone
     */
    public BaseEntity(BaseEntity other) {
        checkNotNull(other);
        setTypeId(other.getTypeId());
        setNbtData(other.getNbtData());
    }

    @Override
    public boolean hasNbtData() {
        return true;
    }

    @Nullable
    @Override
    public CompoundTag getNbtData() {
        return nbtData;
    }

    @Override
    public void setNbtData(@Nullable CompoundTag nbtData) {
        this.nbtData = nbtData;
    }

    /**
     * Get the entity that determines the type of entity.
     *
     * @return the entity ID
     */
    public String getTypeId() {
        return id;
    }

    /**
     * Set the entity ID that determines the type of entity.
     *
     * @param id the id
     */
    public void setTypeId(String id) {
        checkNotNull(id);
        this.id = id;
    }

}
