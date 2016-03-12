package com.minecade.minecraftmaker.function;

import javax.annotation.Nullable;

import com.minecade.minecraftmaker.world.Vector;

/**
 * Tests whether a given vector meets a criteria.
 */
public interface Mask {

    /**
     * Returns true if the criteria is met.
     *
     * @param vector the vector to test
     * @return true if the criteria is met
     */
    boolean test(Vector vector);

    /**
     * Get the 2D version of this mask if one exists.
     *
     * @return a 2D mask version or {@code null} if this mask can't be 2D
     */
    @Nullable
    Mask2D toMask2D();

}
