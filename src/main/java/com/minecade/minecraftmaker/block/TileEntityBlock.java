package com.minecade.minecraftmaker.block;

import com.minecade.minecraftmaker.jnbt.NbtValued;

/**
 * Indicates a block that contains extra data identified as an NBT structure.
 * Compared to a {@link NbtValued}, tile entity blocks also contain an ID.
 *
 * @see NbtValued
 */
public interface TileEntityBlock extends NbtValued {

    /**
     * Return the name of the title entity ID.
     *
     * @return tile entity ID, non-null string
     */
    String getNbtId();

}
