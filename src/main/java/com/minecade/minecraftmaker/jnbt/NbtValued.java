package com.minecade.minecraftmaker.jnbt;

import javax.annotation.Nullable;

/**
 * Indicates an object that contains extra data identified as an NBT structure.
 * This interface is used when saving and loading objects to a serialized
 * format, but may be used in other cases.
 */
public interface NbtValued {
    
    /**
     * Returns whether the block contains NBT data. {@link #getNbtData()}
     * must not return null if this method returns true.
     * 
     * @return true if there is NBT data
     */
    public boolean hasNbtData();

    /**
     * Get the object's NBT data (tile entity data). The returned tag, if
     * modified in any way, should be sent to {@link #setNbtData(CompoundTag)}
     * so that the instance knows of the changes. Making changes without
     * calling {@link #setNbtData(CompoundTag)} could have unintended
     * consequences.
     *
     * <p>{@link #hasNbtData()} must return true if and only if method does
     * not return null.</p>
     * 
     * @return compound tag, or null
     */
    @Nullable
    CompoundTag getNbtData();

    /**
     * Set the object's NBT data (tile entity data).
     * 
     * @param nbtData NBT data, or null if no data
     */
    void setNbtData(@Nullable CompoundTag nbtData);

}
